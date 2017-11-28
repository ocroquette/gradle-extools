package com.github.ocroquette.extools.internal.exec

import com.github.ocroquette.extools.internal.utils.PathResolver
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

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
        getExecClosure(conf)()
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
                if (conf.workingDir != null)
                    workingDir conf.workingDir
            }
        }
    }


    private getAliasesUsed(ExecutionConfiguration conf) {
        if (conf.usingExtools.size() == 0) {
            def pluginConfiguration = project.extensions.extools.configurationState.get()
            return pluginConfiguration.tools.keySet().sort()
        } else
            return conf.usingExtools
    }


    private extendEnvironment(ExecutionConfiguration conf) {
        def pluginConfiguration = project.extensions.extools.configurationState.get()

        def realNamesUsed = []

        getAliasesUsed(conf).each { alias ->
            def realName = pluginConfiguration.tools[alias]
            if (realName == null)
                throw new RuntimeException("Invalid tool or alias: \"$alias\"")
            realNamesUsed.add(realName)
        }

        Set<String> variablesToPrependInEnv = []

        realNamesUsed.each { realName ->
            variablesToPrependInEnv.addAll(pluginConfiguration.configurationOfTool[realName].variablesToPrependInEnv)
        }

        variablesToPrependInEnv.each { variableName ->
            def paths = []

            realNamesUsed.each { realName ->
                pluginConfiguration.configurationOfTool[realName].variables.each { k, v ->
                    if (k == variableName)
                        paths.addAll(v.split(File.pathSeparator))
                }
            }

            def systemValue = System.getenv(variableName)
            if (systemValue != null) {
                systemValue.split(File.pathSeparator).each { it ->
                    paths.add(it)
                }
            }

            conf.environment[variableName] = paths.join(File.pathSeparator)
        }

        Set variablesToSetInEnv = []

        realNamesUsed.each { realName ->
            variablesToSetInEnv.addAll(pluginConfiguration.configurationOfTool[realName].variablesToSetInEnv)
        }

        variablesToSetInEnv.each { variableName ->
            realNamesUsed.each { realName ->
                def value = pluginConfiguration.configurationOfTool[realName].variables[variableName]
                if (value != null) {
                    conf.environment[variableName] = value
                }
            }
        }
    }

    private resolveExecutable(ExecutionConfiguration conf) {
        if (conf.executable == null)
            return // No executable set, do nothing, Exec will complain (IllegalStateException: execCommand == null)

        boolean explicitPathProvided = conf.executable.contains("\\") || conf.executable.contains("/")
        if (!explicitPathProvided) {
            def pathsToSearchForExec = conf.environment["PATH"].split(File.pathSeparator)
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