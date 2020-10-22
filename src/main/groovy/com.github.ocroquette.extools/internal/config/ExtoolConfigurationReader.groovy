package com.github.ocroquette.extools.internal.config

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Reads and parses the conf file of an extool
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
            throw new RuntimeException("Extool conf file doesn't exist: ${confFile.absolutePath}")
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

    private assertValue(def value, def supportedValues, def description) {
        if (!supportedValues.contains(value)) {
            throw new RuntimeException("Unsupported ${description} \"${value}\" at ${confFile.absolutePath}:$lineNumber. Supported are: " + supportedValues.join(","))
        }
    }

    private handleCommand(fields) {

        def nExpectedArguments = 5
        if (fields.size() != nExpectedArguments)
            throw new RuntimeException("Syntax error, expecting ${nExpectedArguments} arguments at ${confFile.absolutePath}:$lineNumber")

        def ACTION_SET = "set"
        def ACTION_PREPEND = "prepend"
        def action = fields[0]
        assertValue(action, [ACTION_SET, ACTION_PREPEND],"action")

        def TYPE_VAR = "var"
        def TYPE_ENV = "env"
        def varType = fields[1]
        assertValue(varType, [TYPE_VAR, TYPE_ENV],"variable type")

        def VALUETYPE_RELPATH = "relpath"
        def VALUETYPE_RELPATH_DEPRECATED = "path"
        def VALUETYPE_ABSPATH = "abspath"
        def VALUETYPE_STRING = "string"
        def valueType = fields[2]
        assertValue(valueType, [VALUETYPE_RELPATH, VALUETYPE_RELPATH_DEPRECATED, VALUETYPE_ABSPATH, VALUETYPE_STRING],"value type")

        def varName = fields[3]

        def value = fields[4]


        if ( valueType == VALUETYPE_RELPATH || valueType == VALUETYPE_RELPATH_DEPRECATED ) {
            value = computeCanonicalPath(value)
        }

        if ( action == ACTION_SET) {
            result.variables.put(varName, value)
            if (varType == TYPE_ENV) {
                if (result.variablesToPrependInEnv.contains(varName)) {
                    throw new RuntimeException("Cannot set value for ${varName} at ${confFile.absolutePath}:$lineNumber, it is already used in a ${ACTION_PREPEND} statement")
                }
                result.variablesToSetInEnv.add(varName)
            }
        }
        else if ( action == ACTION_PREPEND) {
            boolean isPath = ( valueType == VALUETYPE_RELPATH ||
                            valueType == VALUETYPE_RELPATH_DEPRECATED ||
                            valueType == VALUETYPE_ABSPATH )
            String separator = ( isPath ? File.pathSeparator : "")
            result.variables.put(varName, prependString(result.variables[varName], value, separator))
            if (varType == TYPE_ENV) {
                if (result.variablesToSetInEnv.contains(varName)) {
                    throw new RuntimeException("Cannot prepend value for ${varName} at ${confFile.absolutePath}:$lineNumber, it is already used in a ${ACTION_SET} statement")
                }
                result.variablesToPrependInEnv.add(varName)
            }
        }
    }

    private String prependString(String previousValue, String newValue, String separator) {
        if (previousValue == null)
            return newValue
        else {
            return newValue + separator + previousValue
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
