package com.github.ocroquette.extools.internal.config

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

    ExtoolsPluginExtension(Project project) {
        configurationState = project.property(ExtoolsPluginConfiguration)
        configurationState.set(new ExtoolsPluginConfiguration())

        configurationState.get().extractDir = getDefaultExtractDir(project)
        configurationState.get().localCache = getDefaultLocalCache(project)
        configurationState.get().remoteRepositoryUrl = getDefaultRepositoryUrl(project)
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

    void tool(String s) {
        configurationState.get().tools.put(s, s)
    }

    void tool(Map<String, String> map) {
        configurationState.get().tools.putAll(map)
    }

    void tools(Map<String, String> map) {
        tool(map)
    }

    void extractDir(String s) {
        configurationState.get().extractDir = new File(s)
    }

    void localCache(String s) {
        configurationState.get().localCache = new File(s)
    }

    String getValue(String toolAlias, String variableName) {
        ExtoolsPluginConfiguration conf = configurationState.get()
        String realName = conf.tools[toolAlias]
        if (realName == null)
            throw new RuntimeException("Unknown tool alias ${toolAlias}, known tools: " + conf.tools.keySet().join(","))
        ExtoolConfiguration tc = configurationState.get().configurationOfTool[realName]
        String value = tc.variables[variableName]
        if (value == null)
            throw new RuntimeException("Unknown variable ${variableName} for tool alias ${toolAlias}, known variables: " + tc.variables.keySet().join(","))
        return value
    }

    private File getDefaultExtractDir(Project project) {
        def propertyValue = project.properties["extools.extractDir"]
        if ( propertyValue != null )
            return new File(propertyValue)
        else
            return new File(System.properties["user.home"], ".gradle/extools/extractDir")
    }

    private File getDefaultLocalCache(Project project) {
        def propertyValue = project.properties["extools.localCache"]
        if ( propertyValue != null )
            return new File(propertyValue)
        else
            return new File(System.properties["user.home"], ".gradle/extools/localCache")
    }

    private URL getDefaultRepositoryUrl(Project project) {
        def propertyValue = project.properties["extools.repositoryUrl"]
        if ( propertyValue != null )
            return new URL(propertyValue)
        else
            null // It must be set in the build script then
    }
}