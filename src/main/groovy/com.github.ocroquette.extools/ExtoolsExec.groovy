package com.github.ocroquette.extools

import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskAction

class ExtoolsExec extends AbstractExecTask {
    def aliasesUsed = []

    public ExtoolsExec() {
        super(ExtoolsExec.class);
    }

    @Override
    @TaskAction
    protected void exec() {
        extendEnvironment()

        resolveExecutable()

        logger.lifecycle("Executing ${this.executable}...")

        super.exec()
    }

    void usingExtools(String... aliases) {
        aliasesUsed.addAll(aliases)
    }

    private getAliasesUsed() {
        if ( aliasesUsed.size() == 0 ) {
            def pluginConfiguration = project.extensions.extools.configurationState.get()
            return pluginConfiguration.tools.keySet().sort()
        }
        else
            return aliasesUsed
    }

    private extendEnvironment() {
        def pluginConfiguration = project.extensions.extools.configurationState.get()

        def realNamesUsed = []

        getAliasesUsed().each { alias ->
            def realName = pluginConfiguration.tools[alias]
            if (realName == null)
                throw new RuntimeException("Invalid tool or alias: \"$alias\"")
            realNamesUsed.add(realName)
        }

        Set variablesToPrependInEnv = []

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

            environment[variableName] = paths.join(File.pathSeparator)
        }

        Set variablesToSetInEnv = []

        realNamesUsed.each { realName ->
            variablesToSetInEnv.addAll(pluginConfiguration.configurationOfTool[realName].variablesToSetInEnv)
        }

        variablesToSetInEnv.each { variableName ->
            realNamesUsed.each { realName ->
                def value = pluginConfiguration.configurationOfTool[realName].variables[variableName]
                if (value != null) {
                    environment[variableName] = value
                }
            }
        }
    }

    private resolveExecutable() {
        if ( this.executable == null )
            return // No executable set, do nothing, Exec will complain (IllegalStateException: execCommand == null)

        boolean explicitPathProvided = this.executable.contains("\\") || this.executable.contains("/")
        if (!explicitPathProvided) {
            def pathsToSearchForExec = environment["PATH"].split(File.pathSeparator)
            def searchPaths  = pathsToSearchForExec.collect { new File(it) }
            PathResolver resolver = new PathResolver(searchPaths)
            logger.info("Looking for ${this.executable} in $searchPaths")
            def match = resolver.find(this.executable)
            if (match == null)
                throw new RuntimeException("Unable to find ${this.executable} in $searchPaths")
            logger.info("Found ${this.executable} at ${match.absolutePath}")
            this.executable = match.absolutePath
        }
    }
}
