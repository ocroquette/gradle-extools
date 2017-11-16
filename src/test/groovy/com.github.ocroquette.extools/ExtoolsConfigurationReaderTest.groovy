package com.github.ocroquette.extools

import com.github.ocroquette.extools.ExtoolConfigurationReader
import spock.lang.Specification

class ExtoolsConfigurationReaderTest extends Specification {

    static final DIR = "src/test/resources/extools"

    def "must parse single file correctly"() {
        given:
        def reader = new ExtoolConfigurationReader()
        def relativePath = "${DIR}/dummy_1"
        def vars = reader.readFromDir(new File(relativePath))

        expect:
        vars != null
        vars.size() == 4
        vars.get("DUMMY_STRING") == "Value of DUMMY_STRING from dummy_1"
        vars.get("DUMMY1_STRING") == "Value of DUMMY1_STRING"
        vars.get("CMAKE_PREFIX_PATH").contains(new File(relativePath, "cmake").absolutePath)
        vars.get("PATH") == new File(relativePath, "bin").absolutePath
    }
}

