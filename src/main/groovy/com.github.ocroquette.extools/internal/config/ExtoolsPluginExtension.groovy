package com.github.ocroquette.extools.internal.config

import com.github.ocroquette.extools.ExtoolsPlugin
import com.github.ocroquette.extools.internal.config.ExtoolConfiguration
import com.github.ocroquette.extools.internal.config.ExtoolsPluginConfiguration
import org.gradle.api.Project
import org.gradle.api.provider.PropertyState

/**
 * Provides the DSL to configure the plugin itself, e.g. repo URL, tools used...
 *
 * The configuration is stored as PropertyState<ExtoolsPluginConfiguration> in the project
 */
class ExtoolsPluginExtension {
    final PropertyState<ExtoolsPluginConfiguration> configurationState
    Project project
    private overrideWarnings = []

    ExtoolsPluginExtension(Project project) {
        configurationState = project.property(ExtoolsPluginConfiguration)
        configurationState.set(new ExtoolsPluginConfiguration())

        configurationState.get().extractDir = getDefaultExtractDir(project)
        configurationState.get().localCache = getDefaultLocalCache(project)
        configurationState.get().remoteRepositoryUrl = getDefaultRepositoryUrl(project)

        this.project = project
    }

    void remoteRepositoryUrl(String s) {
        s = s + "/" // Force the URL to be a directory
        configurationState.get().remoteRepositoryUrl = new URL(s)
    }

    void tools(List<String> l) {
        l.each { s ->
            tool(s)
        }
    }

    void tool(String alias, String realName) {
        if(isOverriden(alias)) {
            // For overriden extools, replace irrelevant realname by path
            realName = getOverridenPath(alias)
        }
        configurationState.get().tools.put(alias, realName)
    }

    void tool(String s) {
        tool(s, s)
    }

    void tool(Map<String, String> map) {
        map.each { k, v ->
            tool(k, v)
        }
    }

    void tools(Map<String, String> map) {
        tool(map)
    }

    void usingExtools(String alias) {
        // Check we got only valid aliases (will throw exception otherwise):
        resolveAlias(alias)
        configurationState.get().usingAllExtools = false
        configurationState.get().aliasesUsedGlobally.add(alias)
    }

    void usingExtools(String... aliases) {
        aliases.each {
            usingExtools(it)
        }
    }

    void extractDir(String s) {
        configurationState.get().extractDir = new File(s)
    }

    void localCache(String s) {
        configurationState.get().localCache = new File(s)
    }

    String getValue(String toolAlias, String variableName) {
        String value = getValueWithDefault(toolAlias, variableName, null)
        if (value == null) {
            String realName = resolveAlias(toolAlias)
            ExtoolConfiguration tc = configurationState.get().configurationOfTool[realName]
            throw new RuntimeException("Undefined variable ${variableName} for tool alias ${toolAlias}, known variables: " + tc.variables.keySet().join(","))
        }
        return value
    }

    String getValueWithDefault(String toolAlias, String variableName, String defaultValue) {
        String realName = resolveAlias(toolAlias)
        ExtoolConfiguration tc = configurationState.get().configurationOfTool[realName]
        String value = tc.variables[variableName]
        if (value == null)
            return defaultValue
        return value
    }

    private String getOverridenVarName(String toolAlias) {
        String canonicalName = toolAlias.toUpperCase().replaceAll(/[^A-Z0-9\-_]+/, "_")
        return "EXTOOL_${canonicalName}_OVERRIDE"
    }

    boolean isOverriden(String toolAlias) {
        return getOverridenPath(toolAlias) != null
    }

    private String getOverridenPath(String toolAlias) {
        String overrideName = getOverridenVarName(toolAlias)
        String overrideValue = null
        String overridenBy = null

        project.logger.debug("Extools: Checking if the location of \"$toolAlias\" is overriden with \"$overrideName\"")
        if (System.getenv(overrideName) != null) {
            overrideValue = System.getenv(overrideName)
            if (overrideValue != null) {
                overridenBy = "environment variable"
            }
        } else {
            overrideValue = project.getProperties().get(overrideName)
            if (overrideValue != null) {
                overridenBy = "project property"
            }
        }
        if (overrideValue != null && !overrideWarnings.contains(toolAlias)) {
            project.logger.warn("Extools: Location of \"$toolAlias\" overriden by $overridenBy \"$overrideName\" to \"$overrideValue\"")
            overrideWarnings.add(toolAlias) // Warn only once
        }
        return overrideValue
    }

    File getHomeDir(String toolAlias) {
        String overridenPath = getOverridenPath(toolAlias)
        if (overridenPath == null) {
            String realName = resolveAlias(toolAlias)
            return new File(configurationState.get().extractDir, realName)
        } else {
            return new File(overridenPath)
        }
    }

    String resolveAlias(String toolAlias) {
        ExtoolsPluginConfiguration conf = configurationState.get()
        String realName = conf.tools[toolAlias]
        if (realName == null)
            throw new RuntimeException("Unknown tool alias ${toolAlias}, known tools: " + conf.tools.keySet().join(","))
        return realName
    }

    String[] getLoadedAliases() {
        return (configurationState.get().tools.keySet() as String[]).sort()
    }

    def assertExtoolsAreLoaded() {
        if ( ! configurationState.get().areToolsLoaded )
            throw new RuntimeException("The extools are not loaded yet. Missing dependency on ${ExtoolsPlugin.EXTOOLS_LOAD}?")
    }

    String generateEnvironmentScript(String toolAlias) {
        assertExtoolsAreLoaded()
        String realName = resolveAlias(toolAlias)
        ExtoolConfiguration tc = configurationState.get().configurationOfTool[realName]
        def sb = new StringBuilder()
        tc.variablesToSetInEnv.sort().each {
            sb.append(generateEnvironmentLine(it, tc.variables[it], false))
            sb.append("\n")
        }
        tc.variablesToPrependInEnv.sort().each {
            sb.append(generateEnvironmentLine(it, tc.variables[it], true))
            sb.append("\n")
        }
        return sb.toString()
    }

    String generateEnvironmentLine(String varName, String varValue, boolean append) {
        def sb = new StringBuilder()
        if (System.properties['os.name'].toLowerCase().contains('windows')) {
            sb.append("set $varName=" + varValue)
            if(append) {
                sb.append(";")
                sb.append("%$varName%")
            }
        } else {
            sb.append("export $varName=\"" + varValue)
            if(append) {
                sb.append(":")
                sb.append('$' + varName)
            }
            sb.append("\"")
        }
        return sb.toString()
    }

    private File getDefaultExtractDir(Project project) {
        def propertyValue = project.properties["extools.extractDir"]
        if (propertyValue != null)
            return new File(propertyValue)
        else
            return new File(System.properties["user.home"], ".extools/extractDir")
    }

    private File getDefaultLocalCache(Project project) {
        def propertyValue = project.properties["extools.localCache"]
        if (propertyValue != null)
            return new File(propertyValue)
        else
            return new File(System.properties["user.home"], ".extools/localCache")
    }

    private URL getDefaultRepositoryUrl(Project project) {
        def propertyValue = project.properties["extools.repositoryUrl"]
        if (propertyValue != null)
            return new URL(propertyValue)
        else
            null // It must be set in the build script then
    }
}