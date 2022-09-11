package com.github.ocroquette.extools.internal.utils

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.lang.management.ManagementFactory

/**
 * Fetches extools archives from the repository into the provided local directory
 */
class ExtoolsFetcher {
    private URL remoteRepoUrl
    private File targetDir

    private Logger logger = Logging.getLogger(this.getClass().getName())

    /**
     * Creates a fetcher for the given URL
     * @param remoteRepoUrl the URL of the repository, typically with a "file" or "http/s" protocol, maybe null
     * @param targetDir the local directory where to store the files
     *
     * @throws RuntimeException if remoteRepoUrl if null and an Extool needs to be fetched
     */
    ExtoolsFetcher(URL remoteRepoUrl, File targetDir) {
        this.targetDir = targetDir
        this.remoteRepoUrl = remoteRepoUrl
    }

    /**
     * Fetch the given remote file if not available in the target directory
     *
     * @param extoolName the name of the tool, for instance "compilers/gcc-7.1"
     * @throws RuntimeException in case of error (invalid toolID, no URL available...)
     */
    void fetch(String extoolName) {
        if ( extoolName.startsWith("/") || extoolName.startsWith("\\") ) {
            throw new RuntimeException("Invalid tool id: " + extoolName)
        }

        String fileNameExtension = ".zip"

        if ( extoolName.endsWith(fileNameExtension) ) {
            throw new RuntimeException("extools: please remove extension ${fileNameExtension} from extool name ${extoolName}")
        }

        String relativeFilePath = extoolName + fileNameExtension

        File targetFile = new File(targetDir, relativeFilePath)

        if ( targetFile.exists() ) {
            logger.info "Archive for tool ${extoolName} is already available at ${targetFile}"
            return
        }

        if ( remoteRepoUrl == null ) {
            throw new RuntimeException("extools: no repository URL has been provided")
        }

        // See https://stackoverflow.com/a/26787332/1448767
        URL finalUrl = new URL(remoteRepoUrl.getProtocol(), remoteRepoUrl.getHost(), remoteRepoUrl.getPort(), remoteRepoUrl.getFile() + "/" + relativeFilePath, null)

        File tmpFile = TemporaryFileUtils.newTemporaryFileFor(targetDir)
        tmpFile.deleteOnExit() // In case we are interrupted, delete the incomplete data

        logger.lifecycle "Fetching ${extoolName} from ${finalUrl} to ${targetFile}"

        def stream = finalUrl.openStream()
        tmpFile.withOutputStream { out ->
            out << stream
        }

        targetFile.parentFile.mkdirs()
        tmpFile.renameTo(targetFile)
    }
}
