package com.github.ocroquette.extools.internal.launcher

import com.github.ocroquette.extools.ExtoolsPlugin
import com.github.ocroquette.extools.internal.config.ExtoolConfiguration
import com.github.ocroquette.extools.internal.config.ExtoolsPluginConfiguration
import com.github.ocroquette.extools.internal.exec.ExecutionConfiguration
import com.github.ocroquette.extools.internal.exec.LauncherConfiguration
import com.github.ocroquette.extools.internal.utils.PathResolver
import com.github.ocroquette.extools.internal.utils.PathVarUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import sun.misc.Launcher

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Generate Launchers
 */
class LauncherGenerator {
    final private Project project

    private Logger logger = Logging.getLogger(this.getClass().getName())

    private List<String> searchPaths = [] // Used internally

    LauncherGenerator(Project p) {
        this.project = p
    }

    void generate(Closure closure) {
        LauncherConfiguration conf = LauncherConfiguration.fromClosure(closure)
        generate(conf)
    }

    void generate(LauncherConfiguration conf) {
        if ( conf.launcherFile == null )
            throw new RuntimeException("launcherFile is not set")
        def sb = new StringBuilder()

        sb.append(conf.textBefore)
        getAliasesUsed(conf).each {toolAlias ->
            sb.append(project.extensions.extools.generateEnvironmentScript(toolAlias))
        }
        conf.additionalEnvironment.each {k,v ->
            sb.append(project.extensions.extools.generateEnvironmentLine(k, v, false))
        }
        sb.append(conf.textAfter)
        conf.launcherFile.parentFile.mkdirs()
        conf.launcherFile.text = sb.toString()
        // Set executable permission if possible, ignore any error
        conf.launcherFile.setExecutable(true, false)
    }

    private getAliasesUsed(LauncherConfiguration conf) {
        // LinkedHashSet removes duplicates but keeps the insertion order
        LinkedHashSet<String> aliasesUsed = []

        if (!conf.usingExtoolsAppends) {
            // Don't append, use given explicit list
            aliasesUsed.addAll(conf.usingExtools)
        } else {
            // First, add additional tools from exec configuration, if any:
            aliasesUsed.addAll(conf.usingExtools)

            // Then add tools required from the global plugin configuration
            ExtoolsPluginConfiguration pluginConfiguration = project.extensions.extools.configurationState.get()

            aliasesUsed.addAll(pluginConfiguration.aliasesUsedGlobally)

            if (pluginConfiguration.usingAllExtools) {
                aliasesUsed.addAll(pluginConfiguration.tools.keySet())
            }
        }

        return aliasesUsed as List<String>
    }
}
