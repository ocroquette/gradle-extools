package com.github.ocroquette.extools

import com.github.ocroquette.extools.internal.config.ExtoolConfigurationReader
import com.github.ocroquette.extools.testutils.Comparator
import spock.lang.Specification

class ExtoolsConfigurationReaderTest extends Specification {

    static final DIR = "src/test/resources/extools"

    def "must parse single file correctly"() {
        given:
        def reader = new ExtoolConfigurationReader()
        def relativePath = "${DIR}/dummy_1"
        def result = reader.readFromDir(new File(relativePath))

        expect:
        Comparator.compareEnvs([
                DUMMY_STRING: "Value of DUMMY_STRING from dummy_1",
                DUMMY1_STRING: "Value of DUMMY1_STRING",
                DUMMY1_DIR: new File(relativePath, ".").canonicalPath,
                DUMMY1_VAR: "Value of DUMMY1_VAR",
                CMAKE_PREFIX_PATH: (new File(relativePath, "cmake_").canonicalPath
                        + File.pathSeparator + new File(relativePath, "cmake").canonicalPath),
                PATH: ( "absolute_path" + File.pathSeparator +
                        new File(relativePath, "bin2").canonicalPath + File.pathSeparator +
                        new File(relativePath, "bin").canonicalPath )
        ], result.variables, []) == ""
        Comparator.compareAsSets(["DUMMY1_STRING", "DUMMY_STRING", "DUMMY1_DIR"], result.variablesToSetInEnv) == ""
        Comparator.compareAsSets(["CMAKE_PREFIX_PATH", "PATH"], result.variablesToPrependInEnv)  == ""
    }
}

