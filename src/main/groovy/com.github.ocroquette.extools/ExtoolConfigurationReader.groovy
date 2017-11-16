package com.github.ocroquette.extools

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging


/**
 * Reads and parses the configuration file of an extool
 */
class ExtoolConfigurationReader {

    public static final String CONF_FILE_NAME = "extools.conf"
    private TreeMap<String, String> variables


    private Logger logger = Logging.getLogger(this.getClass().getName())

    private File root
    private File confFile
    private int lineNumber

    TreeMap<String, String> readFromDir(File extoolRoot) {
        this.root = extoolRoot
        this.variables = new TreeMap<String, String>()

        confFile = new File(root, CONF_FILE_NAME)
        lineNumber = 0

        if (confFile.isFile()) {
            readFromFile(confFile)
        } else {
            throw new RuntimeException("Extool configuration file doesn't exist: ${confFile.absolutePath}")
        }
        return this.variables
    }

    private readFromFile(File confFile) {
        confFile.text.eachLine { line ->
            lineNumber++

            // Remove comments:
            line = line.replaceAll("#.*", "")

            if (line.matches("\\s*"))
                return

            // Parse line:
            def fields = line.split(";")

            def previousValues = variables.clone()
            handleCommand(fields)
        }
    }

    private handleCommand(fields) {
        def command = fields[0]

        switch (command) {
            case "setPath":
                if (fields.size() != 3)
                    throw new RuntimeException("${command} requires 3 arguments at $lineNumber in ${confFile.absolutePath}")
                variables.put(fields[1], computeAbsolutePath(fields[2]))
                break
            case "appendPath":
                if (fields.size() != 3)
                    throw new RuntimeException("${command} requires 3 arguments at $lineNumber in ${confFile.absolutePath}")
                variables.put(fields[1], appendPath(variables[fields[1]], fields[2]))
                break
            case "appendString":
                if (fields.size() != 3)
                    throw new RuntimeException("${command} requires 3 arguments at $lineNumber in ${confFile.absolutePath}")
                variables.put(fields[1], fields[2])
                break
            default:
                throw new RuntimeException("Unsupported command ${command} at line $lineNumber in ${confFile.absolutePath}")

        }
    }

    private String appendPath(String previousValue, String newValue) {
        String absolutePath = computeAbsolutePath(newValue)
        if (previousValue == null)
            return absolutePath
        else {
            return previousValue + File.pathSeparator + absolutePath
        }
    }

    private String computeAbsolutePath(String relativePath) {
        File f = new File(root, relativePath)
        if (!f.exists()) {
            logger.debug("Referenced path does not exit ${f.absolutePath} at line $lineNumber in ${confFile.absolutePath}")
        }
        return f.absolutePath
    }
}
