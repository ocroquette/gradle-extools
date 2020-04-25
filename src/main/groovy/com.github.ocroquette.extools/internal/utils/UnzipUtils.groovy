package com.github.ocroquette.extools.internal.utils

import org.gradle.api.Project
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView

class UnzipUtils {


    /**
     * Unzip the given file to the given directory
     *
     * In general and on Windows, it uses the standard Gradle way (ant.unzip).
     * On macOS, it uses /usr/bin/unzip, which is part of the standard OS and can deal with symbolic links and
     * permissions, assuming the ZIP file has been generated in a compatible way (for instance, with /usr/bin/zip --symlinks)
     *
     * @param project
     * @param zipFile
     * @param destDir
     */
    static void unzip(Project project, File zipFile, File destDir) {

        final String IMPL_MACOS = "macos"
        final String IMPL_ANT = "ant"

        String implementation
        // May be we could add a way to override the default method using a project property here
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            implementation = IMPL_MACOS
        } else {
            implementation = IMPL_ANT
        }

        project.logger.debug("Unzipping \"${zipFile}\" to \"${destDir}\" with implementation: " + implementation)

        if(implementation == IMPL_ANT) {
            unzipWithAnt(project, zipFile, destDir)
        }
        else {
            unzipOnMacOs(project, zipFile, destDir)
        }
    }

    static void unzipWithAnt(Project project, File zipFile, File destDir) {

        project.ant.unzip(src: zipFile,
                dest: destDir)

        destDir.eachFileRecurse(groovy.io.FileType.FILES) {
            // Unfortunately, ant.unzip doesn't restore the file permissions, so programs and scripts will
            // fail to start on OSes like Linux.
            // As a workaround, set all files as executables.
            it.setExecutable(true)
        }

        // ant.unzip() does not set the last access time, which can cause problem with some tools
        // later on. So set it explicitly for every file.
        def now = java.time.Instant.now()
        destDir.eachFileRecurse {
            Files.getFileAttributeView(it.toPath(), BasicFileAttributeView.class).setTimes(null, now, null)
        }
    }

    /**
     * Unzip on macOS
     *
     * Uses /usr/bin/unzip, which is part of the standard OS and can deal with symbolic links and
     * restore permissions, assuming the ZIP file has been generated in a compatible way.
     *
     * @param project
     * @param zipFile
     * @param destDir
     */
    static void unzipOnMacOs(Project project, File zipFile, File destDir) {

        destDir.mkdirs()

        project.exec {
            commandLine "/usr/bin/unzip",
                    "-q",                       // Quiet, otherwise every file path is printed out
                    zipFile.absolutePath,       // Path of ZIP file
                    "-d", destDir.absolutePath  // Extract dir
        }
    }
}
