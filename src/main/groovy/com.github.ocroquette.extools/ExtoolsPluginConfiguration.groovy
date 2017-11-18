package com.github.ocroquette.extools

class ExtoolsPluginConfiguration {
    URL remoteRepositoryUrl = null

    /**
     * All extools used as map of aliases to real names
     */
    final tools = [:]

    File localCache
    File extractDir

    /**
     * All ExtoolConfiguraiton's referenced by tool real name
     */
    final configurationOfTool = [:]
}