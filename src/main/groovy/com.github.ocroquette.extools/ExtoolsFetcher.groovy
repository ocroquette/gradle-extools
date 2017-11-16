package com.github.ocroquette.extools

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Fetches extools archives into the provided local directory
 */
class ExtoolsFetcher {
    private URL remoteRepoUrl
    private File targetDir

    private Logger logger = Logging.getLogger(this.getClass().getName())

    /**
     * Creates a fetcher for the given URL
     * @param remoteRepoUrl the URL of the repository, typically with a "file" or "http/s" protocol
     * @param targetDir the local directory where to store the files
     */
    ExtoolsFetcher(URL remoteRepoUrl, File targetDir) {
        this.targetDir = targetDir
        this.remoteRepoUrl = remoteRepoUrl
    }

    /**
     * Fetch the given remote file if not available in the target directory
     *
     * @param toolId the identifier of the tool, for instance "gcc-7.1" or "compilers/gcc-7.1"
     */
    void fetch(String toolId) {
        String fileName = toolId + ".ext"

        File targetFile = new File(targetDir, fileName)

        if ( targetFile.exists() ) {
            logger.info "Archive for tool ${toolId} is already available at ${targetFile}"
            return
        }

        targetFile.parentFile.mkdirs()

        URL finalUrl = new URL(remoteRepoUrl, fileName)

        File tmpFile = new File(targetDir, fileName + ".part")

        logger.lifecycle "Fetching ${toolId} from ${finalUrl} to ${targetFile}"

        def stream = finalUrl.openStream()
        tmpFile.withOutputStream { out ->
            out << stream
        }

        tmpFile.renameTo(targetFile)
    }
}
