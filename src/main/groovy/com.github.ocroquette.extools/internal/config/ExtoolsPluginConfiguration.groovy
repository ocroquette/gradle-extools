package com.github.ocroquette.extools.internal.config

class ExtoolsPluginConfiguration {
    URL remoteRepositoryUrl = null

    /**
     * All extools used, as map of aliases to real names
     *
     * For instance: "gcc": "mingw-gcc-win32-v7.2.1"
     */
    final tools = [:]

    /**
     * Directory to store the downloaded extools archive to
     */
    File localCache

    /**
     * Directory to extract the local extools for use
     */
    File extractDir

    /**
     * Maps the tool real name (e.g. "mingw-gcc-win32-v7.2.1") to the corresponding ExtoolConfiguration
     */
    final configurationOfTool = [:]

    /**
     * Aliases to add implicitly when executing
     */
    final List<String> aliasesUsedGlobally = []
}