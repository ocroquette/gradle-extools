package com.github.ocroquette.extools

import com.github.ocroquette.extools.internal.config.ExtoolConfiguration
import com.github.ocroquette.extools.internal.config.ExtoolConfigurationReader
import com.github.ocroquette.extools.internal.config.ExtoolsPluginConfiguration
import com.github.ocroquette.extools.internal.config.ExtoolsPluginExtension
import com.github.ocroquette.extools.internal.exec.Executor
import com.github.ocroquette.extools.internal.utils.ExtoolsFetcher
import com.github.ocroquette.extools.internal.utils.TemporaryFileUtils
import com.github.ocroquette.extools.internal.utils.UnzipUtils
import com.github.ocroquette.extools.tasks.ExtoolExec
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.github.ocroquette.extools.tasks.ExtoolExecCli


class ExtoolsPlugin implements Plugin<Project> {

    public static final String EXTOOLS_FETCH = 'extoolsFetch'
    public static final String EXTOOLS_EXTRACT = 'extoolsExtract'
    public static final String EXTOOLS_LOAD = 'extoolsLoad'
    public static final String EXTOOLS_INFO = 'extoolsInfo'
    public static final String EXTOOLS_EXEC = 'extoolsExec'

    public static final String EXTOOLS_GROUP = 'Extools'

    void apply(Project project) {
        addExtension(project)

        project.task(EXTOOLS_FETCH) {
            description "Fetches all referenced extools from the remote repository"
            group EXTOOLS_GROUP
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
                configuration.tools.each { alias, realName ->
                    if(!project.extensions.extools.isOverriden(alias)) {
                        fetcher.fetch(realName)
                    }
                }
            }
        }

        project.task(EXTOOLS_EXTRACT) {
            description "Extracts all referenced extools that have been fetched previously into the local extraction directory"
            group EXTOOLS_GROUP
            dependsOn EXTOOLS_FETCH
            doLast {
                ExtoolsPluginConfiguration configuration = project.extensions.extools.configurationState.get()

                def ant = new AntBuilder()

                configuration.tools.each { alias, realName ->
                    if(project.extensions.extools.isOverriden(alias))
                        return
                    File dest = new File(configuration.extractDir, realName)
                    if (dest.exists()) {
                        logger.info("${dest} is already available")
                    } else {
                        File tmpDir = TemporaryFileUtils.newTemporaryFileFor(dest)

                        // the temporary location is thread specific, so this is safe:
                        tmpDir.deleteDir()
                        tmpDir.parentFile.mkdirs()
                        tmpDir.deleteOnExit() // In case we are interrupted, delete the incomplete data

                        File src = new File(configuration.localCache, "${realName}.zip")
                        // ant.unzip is already logging to "lifecycle"
                        logger.info("Extracting ${src} to ${tmpDir}")

                        UnzipUtils.unzip(project, src, tmpDir)

                        tmpDir.renameTo(dest)
                    }
                }
            }
        }
        project.task(EXTOOLS_LOAD) {
            description "Loads the meta-information of all referenced extools from the local extraction directory"
            group EXTOOLS_GROUP
            dependsOn EXTOOLS_EXTRACT
            doLast {
                ExtoolsPluginConfiguration configuration = project.extensions.extools.configurationState.get()
                ExtoolConfigurationReader reader = new ExtoolConfigurationReader()

                configuration.tools.each { alias, realName ->
                    File dir = project.extensions.extools.getHomeDir(alias)
                    if (!dir.isDirectory()) {
                        throw new RuntimeException("External tool directory doesn't exist: ${dir.absolutePath}")
                    }
                    logger.info("extools: Loading tool conf from ${dir.absolutePath}")

                    ExtoolConfiguration conf = reader.readFromDir(dir)

                    configuration.configurationOfTool[realName] = conf

                }

                configuration.areToolsLoaded = true
            }
        }

        project.task(EXTOOLS_INFO) {
            description "Shows the meta-information of all referenced extools"
            group EXTOOLS_GROUP
            dependsOn EXTOOLS_LOAD
            doLast {
                ExtoolsPluginConfiguration configuration = project.extensions.extools.configurationState.get()
                ExtoolConfigurationReader reader = new ExtoolConfigurationReader()

                println "globalconfig:"
                println "  repositoryUrl: " + configuration.remoteRepositoryUrl
                println "  localCache: " + configuration.localCache
                println "  extractDir: " + configuration.extractDir
                println "tools:"

                configuration.tools.each { alias, realName ->
                    println "  -"
                    println "    alias: $alias"
                    println "    realname: $realName"
                    println "    variables:"
                    ExtoolConfiguration toolConf = configuration.configurationOfTool[realName]
                    toolConf.variables.each { variable, value ->
                        println "      $variable: $value"
                    }
                    println "    variablesToSetInEnv:"
                    toolConf.variablesToSetInEnv.each { variable ->
                        println "      - $variable"
                    }
                    println "    variablesToPrependInEnv:"
                    toolConf.variablesToPrependInEnv.each { variable ->
                        println "      - $variable"
                    }
                }
            }
        }

        project.task(EXTOOLS_EXEC, type: ExtoolExecCli) {
            description "Executes the command line specified"
            group EXTOOLS_GROUP
            dependsOn EXTOOLS_LOAD
        }

        // Add an implicit dependency to 'extoolsLoad' for all ExtoolExec tasks
        project.afterEvaluate {
            project.tasks.each { task ->
                if ( task instanceof ExtoolExec) {
                    task.dependsOn EXTOOLS_LOAD
                }
            }
        }

        project.extensions.extraProperties.set("extoolexec", { Closure c ->
            new Executor(project).executeConfiguration(c)
        })
    }

    void addExtension(Project project) {
        def extension = project.extensions.create('extools', ExtoolsPluginExtension, project)
    }
}
