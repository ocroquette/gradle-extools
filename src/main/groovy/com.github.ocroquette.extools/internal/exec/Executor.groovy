package com.github.ocroquette.extools.internal.exec

import com.github.ocroquette.extools.ExtoolsPlugin
import com.github.ocroquette.extools.internal.config.ExtoolsPluginConfiguration
import com.github.ocroquette.extools.internal.utils.PathResolver
import com.github.ocroquette.extools.internal.utils.PathVarUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Interprets and executes extool calls provided as closures
 */
class Executor {
    final private Project project

    private Logger logger = Logging.getLogger(this.getClass().getName())

    Executor(Project p) {
        this.project = p
    }

    void executeConfiguration(Closure closure) {
        ExecutionConfiguration conf = ExecutionConfiguration.fromClosure(closure)
        executeConfiguration(conf)
    }

    void executeConfiguration(ExecutionConfiguration conf) {
        Closure c = getExecClosure(conf)
        if (conf.runInBackground()) {
            Executors.newSingleThreadExecutor().submit(c)
        } else {
            c()
        }
    }

    private Closure getExecClosure(Closure closure) {
        ExecutionConfiguration conf = ExecutionConfiguration.fromClosure(closure)
        return getExecClosure(conf)
    }

    private Closure getExecClosure(ExecutionConfiguration conf) {
        extendEnvironment(conf)

        resolveExecutable(conf)

        return { ->
            project.exec {
                executable conf.executable
                args conf.args
                if (conf.environment != null)
                    environment conf.environment
                if (conf.errorOutput != null)
                    errorOutput conf.errorOutput
                if (conf.ignoreExitValue != null)
                    ignoreExitValue conf.ignoreExitValue
                if (conf.standardInput != null)
                    standardInput conf.standardInput
                if (conf.standardOutput != null)
                    standardOutput conf.standardOutput
                if (conf.workingDir != null) {
                    if (!conf.workingDir.isDirectory()) {
                        throw new RuntimeException("Invalid working directory: \"" + conf.workingDir + "\" for: \"" + conf.executable + "\"")
                    }
                    workingDir conf.workingDir
                }
            }
        }
    }


    private getAliasesUsed(ExecutionConfiguration conf) {
        List<String> aliasesUsed = []

        if (!conf.usingExtoolsAppends) {
            aliasesUsed.addAll(conf.usingExtools)
            return aliasesUsed
        }

        ExtoolsPluginConfiguration pluginConfiguration = project.extensions.extools.configurationState.get()

        if (pluginConfiguration.usingAllExtools) {
            aliasesUsed.addAll(pluginConfiguration.tools.keySet())
            return aliasesUsed
        }

        aliasesUsed.addAll(conf.usingExtools)
        aliasesUsed.addAll(pluginConfiguration.aliasesUsedGlobally)
        aliasesUsed.unique(true)

        return aliasesUsed
    }


    private extendEnvironment(ExecutionConfiguration conf) {
        def pluginConfiguration = project.extensions.extools.configurationState.get()

        if (!pluginConfiguration.areToolsLoaded)
            throw new RuntimeException("The extools are not loaded yet. Missing dependency on ${ExtoolsPlugin.EXTOOLS_LOAD}?")

        def realNamesUsed = []

        getAliasesUsed(conf).each { alias ->
            def realName = pluginConfiguration.tools[alias]
            if (realName == null) {
                def actualAlias = pluginConfiguration.tools.find { it.value == alias }?.key
                def errorMessage = (actualAlias == null
                        ? "Invalid extool name or alias: \"$alias\""
                        : "Use alias \"$actualAlias\" instead of real name \"$alias\"" );
                throw new RuntimeException(errorMessage)
            }
            realNamesUsed.add(realName)
        }

        Set<String> variablesToPrependInEnv = []

        variablesToPrependInEnv.addAll(conf.prependEnvPath.keySet())
        realNamesUsed.each { realName ->
            variablesToPrependInEnv.addAll(pluginConfiguration.configurationOfTool[realName].variablesToPrependInEnv)
        }

        variablesToPrependInEnv.each { variableName ->
            def paths = []

            if (conf.prependEnvPath[variableName] != null) {
                paths.addAll(conf.prependEnvPath[variableName].split(File.pathSeparator))
            }

            realNamesUsed.each { realName ->
                pluginConfiguration.configurationOfTool[realName].variables.each { k, v ->
                    if (k == variableName)
                        paths.addAll(v.split(File.pathSeparator))
                }
            }

            String systemCase = getSystemCase(variableName)

            def systemValue = System.getenv(systemCase)
            if (systemValue != null) {
                systemValue.split(File.pathSeparator).each { it ->
                    paths.add(it)
                }
            }

            conf.environment[systemCase] = paths.join(File.pathSeparator)
        }

        Set variablesToSetInEnv = []

        realNamesUsed.each { realName ->
            variablesToSetInEnv.addAll(pluginConfiguration.configurationOfTool[realName].variablesToSetInEnv)
        }

        variablesToSetInEnv.each { variableName ->
            realNamesUsed.each { realName ->
                def value = pluginConfiguration.configurationOfTool[realName].variables[variableName]
                if (value != null) {
                    String systemCase = getSystemCase(variableName)
                    conf.environment[systemCase] = value
                }
            }
        }
    }

    private getSystemCase(String variableName) {
        for (String systemVar : System.getenv().keySet()) {
            if (systemVar.toLowerCase() == variableName.toLowerCase())
                return systemVar
        }
        return variableName
    }

    private resolveExecutable(ExecutionConfiguration conf) {
        if (conf.executable == null)
            return // No executable set, do nothing, Exec will complain (IllegalStateException: execCommand == null)

        boolean explicitPathProvided = conf.executable.contains("\\") || conf.executable.contains("/")
        if (!explicitPathProvided) {
            def systemPathVariableName = PathVarUtils.getSystemPathVariableName()
            def pathsToSearchForExec = conf.environment[systemPathVariableName].split(File.pathSeparator)
            def searchPaths = pathsToSearchForExec.collect { new File(it) }
            PathResolver resolver = new PathResolver(searchPaths)
            logger.info("Looking for ${conf.executable} in $searchPaths")
            def match = resolver.find(conf.executable)
            if (match == null)
                throw new RuntimeException("Unable to find ${conf.executable} in $searchPaths")
            logger.info("Found ${conf.executable} at ${match.absolutePath}")
            conf.executable = match.absolutePath
        }
    }

}
