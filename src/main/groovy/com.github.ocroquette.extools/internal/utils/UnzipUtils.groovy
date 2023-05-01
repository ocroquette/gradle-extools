package com.github.ocroquette.extools.internal.utils

import org.gradle.api.Project

class UnzipUtils {


    /**
     * Unzip the given file to the given directory
     *
     * On Windows, it uses the standard Gradle way (ant.unzip).
     * On other systems, it uses /usr/bin/unzip, and is supposed to deal with symbolic links and
     * permissions, assuming the ZIP file has been generated in a compatible way
     * (for instance, with /usr/bin/zip --symlinks).
     *
     * @param project
     * @param zipFile
     * @param destDir
     */
    static void unzip(Project project, File zipFile, File destDir) {

        final String IMPL_ANT = "ant"
        final String IMPL_UNZIP = "unzip"

        String implementation
        // May be we could add a way to override the default method using a project property here
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            implementation = IMPL_ANT
        } else {
            implementation = IMPL_UNZIP
        }

        project.logger.debug("Unzipping \"${zipFile}\" to \"${destDir}\" with implementation: " + implementation)

        if(implementation == IMPL_ANT) {
            unzipWithAnt(project, zipFile, destDir)
        }
        else {
            unzipWithUnzip(project, zipFile, destDir)
        }
    }

    static void unzipWithAnt(Project project, File zipFile, File destDir) {

        project.ant.unzip(src: zipFile,
                dest: destDir)

        // Unfortunately, ant.unzip doesn't restore the file permissions, so programs and scripts will
        // fail to start on OSes Linux.
        // As a workaround, set all files as executables.
        destDir.eachFileRecurse(groovy.io.FileType.FILES) {
            it.setExecutable(true)
        }
    }

    /**
     * Unzip with the unzip executable
     *
     * Uses /usr/bin/unzip, which is part of the standard OS and can deal with symbolic links and
     * restore permissions, assuming the ZIP file has been generated in a compatible way.
     *
     * @param project
     * @param zipFile
     * @param destDir
     */
    static void unzipWithUnzip(Project project, File zipFile, File destDir) {
        def unzipExe = new File("/usr/bin/unzip")
        if ( ! unzipExe.exists() ) {
            throw new RuntimeException("Unzip executable not found: \"${unzipExe}\"")
        }
        destDir.mkdirs()
        project.exec {
            commandLine unzipExe.absolutePath,
                    "-q",                       // Quiet, otherwise every file path is printed out
                    zipFile.absolutePath,       // Path of ZIP file
                    "-d", destDir.absolutePath  // Extract dir
        }
    }
}
