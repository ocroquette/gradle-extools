package com.github.ocroquette.extools

import org.gradle.api.Plugin
import org.gradle.api.Project


class ExtoolsPlugin implements Plugin<Project> {

    public static final String EXTOOLS_FETCH = 'extoolsFetch'
    public static final String EXTOOLS_EXTRACT = 'extoolsExtract'
    public static final String EXTOOLS_LOAD = 'extoolsLoad'

    void apply(Project project) {
        addExtension(project)

        project.task(EXTOOLS_FETCH) {
            description "Fetches all referenced extools from the remote repository"
            group "Extools"
            doLast {
                ExtoolsPluginConfiguration configuration = project.extensions.extools.configurationState.get()

                if ( configuration.tools.size() == 0 )
                    return

                URL remoteRepositoryUrl = configuration.remoteRepositoryUrl
                logger.debug "remoteRepositoryUrl=" + remoteRepositoryUrl

                if ( remoteRepositoryUrl == null ) {
                    throw new RuntimeException("extools: no repository URL has been provided")
                }

                logger.info("Fetching extools, localCache=${configuration.localCache} remoteRepositoryUrl=${remoteRepositoryUrl}")

                def fetcher = new ExtoolsFetcher(remoteRepositoryUrl, configuration.localCache)
                configuration.tools.values().each { realName ->
                    fetcher.fetch(realName)
                }
            }
        }

        project.task(EXTOOLS_EXTRACT) {
            description "Extracts all referenced extools that have been fetched previously into the local extraction directory"
            group "Extools"
            dependsOn EXTOOLS_FETCH
            doLast {
                ExtoolsPluginConfiguration configuration = project.extensions.extools.configurationState.get()

                def ant = new AntBuilder()

                configuration.tools.each { alias, realName ->
                    File dest = new File(configuration.extractDir, realName)
                    if (dest.exists()) {
                        logger.info("${dest} is already available")
                    } else {
                        dest.mkdirs()
                        File src = new File(configuration.localCache, "${realName}.ext")
                        // ant.unzip is already logging to "lifecycle"
                        logger.info("Extracting ${src} to ${dest}")
                        ant.unzip(src: src,
                                dest: dest,
                                overwrite: "false")
                        // Unfortunately, ant.unzip doesn't restore the file permissions
                        // As a workaround, set all files as executables
                        dest.eachFileRecurse(groovy.io.FileType.FILES) {
                            it.setExecutable(true)
                        }
                    }
                }
            }
        }
        project.task(EXTOOLS_LOAD) {
            description "Loads the meta-information of all referenced extools from the local extraction directory"
            group "Extools"
            dependsOn EXTOOLS_EXTRACT
            doLast {
                ExtoolsPluginConfiguration configuration = project.extensions.extools.configurationState.get()
                ExtoolConfigurationReader reader = new ExtoolConfigurationReader()

                configuration.tools.each { alias, realName ->
                    File dir = new File(configuration.extractDir, realName)
                    if (!dir.isDirectory()) {
                        throw new RuntimeException("External tool directory doesn't exist: ${dir.absolutePath}")
                    }
                    logger.info("extools: Loading tool configuration from ${dir.absolutePath}")

                    ExtoolConfiguration conf = reader.readFromDir(dir)

                    configuration.configurationOfTool[realName] = conf
                }
            }
        }

        // Add an implicit dependency to 'extoolsLoad' for all ExtoolsExec tasks
        project.afterEvaluate {
            project.tasks.each { task ->
                if ( task instanceof ExtoolsExec) {
                    task.dependsOn EXTOOLS_LOAD
                }
            }
        }
    }

    void addExtension(Project project) {
        def extension = project.extensions.create('extools', ExtoolsPluginExtension, project)
    }
}
