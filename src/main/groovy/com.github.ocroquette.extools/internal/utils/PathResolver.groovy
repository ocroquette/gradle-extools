package com.github.ocroquette.extools.internal.utils

/**
 * Finds executable in the PATH
 *
 * Reproduces the native mechanisms that Windows and Unix-like systems use to find executable in the directories
 * specified in the PATH variables.
 *
 * Java doesn't allow to overwrite the PATH variable of the current process, and we  actually don't want to do that
 * anyway, since it is used for the whole process and any new child processes.
 * Theoretically, we could also use ProcessBuilder to start a CMD.EXE or bash process to force the system to resolve
 * the PATH, but reproducing the system algorithms is probably easier to understand, test and debug. It also avoids
 * an unnecessary process.
 */
class PathResolver {

    enum OperatingSystem {
        WINDOWS,
        UNIX
    }

    def paths
    OperatingSystem forcedOs = null

    // Required for testing the Windows specific code:
    def PATHEXT = System.getenv("PATHEXT")

    /**
     * Creates a resolver from the given PATH variable value.
     *
     * @param pathVariable list of directories to search separated by ";" (Windows) or ";" (Unix)
     */
    PathResolver(String pathVariable) {
        this.paths = pathVariable.split(File.pathSeparator).collect {new File(it)}
    }

    /**
     * Creates a resolver from the given list of paths
     *
     * @param pathList list of directories to search
     */
    PathResolver(List<File> pathList) {
        this.paths = pathList
    }

    /**
     * Force the resoler to assume the given OS.
     *
     * Used for testing only.
     *
     * @param os the OS to assume
     */
    void forceOperatingSystem(OperatingSystem os) {
        this.forcedOs = os
    }

    /**
     * Find a program based on it's name
     * @param requestedName the program's name
     * @return the program file, or null if not found
     */
    File find(final String requestedName) {
        for (File dir: paths) {
            def candidatesList = dir.listFiles()
            candidatesList = candidatesList.findAll { it.isFile() }
            def match = match(requestedName, candidatesList)
            if ( match != null )
                return match
        }
        return null
    }

    private File match(String requestedName, def candidatesList) {
        OperatingSystem requestedOs
        if ( this.forcedOs != null )
            requestedOs  = this.forcedOs
        else
            requestedOs = ( System.getProperty("os.name").toLowerCase().contains("windows") ?
                    OperatingSystem.WINDOWS : OperatingSystem.UNIX )

        switch(requestedOs) {
            case OperatingSystem.WINDOWS:
                return matchWindows(requestedName, candidatesList)
            case OperatingSystem.UNIX:
                return matchUnix(requestedName, candidatesList)
        }
        throw new RuntimeException("Not implemented: " + requestedOs)
    }

    private File matchUnix(String requestedName, def candidatesList) {
        return candidatesList.find { it.name == requestedName && it.canExecute()}
    }

    private File matchWindows(String requestedName, def candidatesList) {
        def extList = PATHEXT.split(";")

        def exactMatch = candidatesList.find { it.name.equalsIgnoreCase(requestedName)}
        if ( exactMatch != null ) {
            boolean supportedExtension = extList.findAll { ext -> exactMatch.name.toLowerCase().endsWith(ext.toLowerCase())}.size() > 0
            if (supportedExtension)
                return exactMatch
        }

        for(String extension: extList) {
            String testName = requestedName + extension
            for (File candidate: candidatesList) {
                if (candidate.getName().equalsIgnoreCase(testName))
                    return candidate
            }
        }
        return null
    }

}
