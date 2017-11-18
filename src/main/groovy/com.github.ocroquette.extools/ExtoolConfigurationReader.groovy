package com.github.ocroquette.extools

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Reads and parses the configuration file of an extool
 */
class ExtoolConfigurationReader {

    public static final String CONF_FILE_NAME = "extools.conf"

    ExtoolConfiguration result

    private Logger logger = Logging.getLogger(this.getClass().getName())

    private File root
    private File confFile
    private int lineNumber

    ExtoolConfiguration readFromDir(File extoolRoot) {
        this.root = extoolRoot
        this.result = new ExtoolConfiguration()

        confFile = new File(root, CONF_FILE_NAME)
        lineNumber = 0

        if (confFile.isFile()) {
            readFromFile(confFile)
        } else {
            throw new RuntimeException("Extool configuration file doesn't exist: ${confFile.absolutePath}")
        }
        return result
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

            handleCommand(fields)
        }
    }

    private checkArgument(def value, def supportedValues, def description) {
        if (!supportedValues.contains(value)) {
            throw new RuntimeException("Unsupported ${description} \"${value}\" at ${confFile.absolutePath}:$lineNumber. Supported are: " + supportedValues.join(","))
        }
    }

    private handleCommand(fields) {

        def nExpectedArguments = 5
        if (fields.size() != nExpectedArguments)
            throw new RuntimeException("Syntax error, expecting ${nExpectedArguments} arguments at ${confFile.absolutePath}:$lineNumber")

        def ACTION_SET = "set"
        def ACTION_APPEND = "append"
        def action = fields[0]
        checkArgument(action, [ACTION_SET, ACTION_APPEND],"action")

        def TYPE_VAR = "var"
        def TYPE_ENV = "env"
        def varType = fields[1]
        checkArgument(varType, [TYPE_VAR, TYPE_ENV],"variable type")

        def VALUETYPE_PATH = "path"
        def VALUETYPE_STRING = "string"
        def valueType = fields[2]
        checkArgument(valueType, [VALUETYPE_PATH, VALUETYPE_STRING],"value type")

        def varName = fields[3]

        def value = fields[4]


        if (valueType == VALUETYPE_PATH) {
            value = computeCanonicalPath(value)
        }

        if ( action == ACTION_SET) {
            result.variables.put(varName, value)
            if (varType == TYPE_ENV) {
                if (result.variablesToAppendInEnv.contains(varName)) {
                    throw new RuntimeException("TODO")
                }
                result.variablesToSetInEnv.add(varName)
            }
        }
        else if ( action == ACTION_APPEND) {
            String separator = ( valueType == VALUETYPE_PATH ? File.pathSeparator : "")
            result.variables.put(varName, appendString(result.variables[varName], value, separator))
            if ( result.variablesToSetInEnv.contains(varName)) {
                throw new RuntimeException("TODO")
            }
            result.variablesToAppendInEnv.add(varName)
        }
    }

    private String appendString(String previousValue, String newValue, String separator) {
        if (previousValue == null)
            return newValue
        else {
            return previousValue + separator + newValue
        }
    }

    private String computeCanonicalPath(String relativePath) {
        File f = new File(root, relativePath)
        if (!f.exists()) {
            logger.debug("Referenced path does not exit ${f.canonicalPath} at line $lineNumber in ${confFile.canonicalPath}")
        }
        return f.canonicalPath
    }
}