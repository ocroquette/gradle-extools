package com.github.ocroquette.extools.internal.utils

import java.lang.management.ManagementFactory

class TemporaryFileUtils {
    /**
     * Generate a temporary file based on the given file.
     *
     * Use it to generate temporary locations to use for file operations that might be interupted and let incomplete
     * data on the file system, for instance downloads or extraction.
     *
     * The temporary file contains the current process and thread IDs to avoid conflicts. It is located in the same
     * directory as the provided file.
     *
     * No file operation is actually performed, it just generates a path.
     * It works with directories and files.
     *
     * @param file the file to base the new name on.
     * @return the generated temporary path
     */
    static File newTemporaryFileFor(File file) {
        String pid = ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^a-zA-Z0-9]+", "_")
        long threadId = Thread.currentThread().getId()
        return new File(file.getParentFile(), file.getName() + ".${pid}.${threadId}.part")
    }
}
