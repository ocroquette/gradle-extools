package com.github.ocroquette.extools.utils

import com.github.ocroquette.extools.internal.utils.TemporaryFileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class TemporaryFileUtilsTest extends Specification {
    @Rule
    TemporaryFolder temporaryFolder

    def "temporary files must be unique in each thread and must start with original file path"() {
        given:
        File temporaryFile = temporaryFolder.newFile()

        when:
        File file1
        File file2
        File file3
        Thread.start {
            file1 = TemporaryFileUtils.newTemporaryFileFor(temporaryFile)
        }.join()
        Thread.start {
            file2 = TemporaryFileUtils.newTemporaryFileFor(temporaryFile)
        }.join()
        file3 = TemporaryFileUtils.newTemporaryFileFor(temporaryFile)

        then:
        def inputList = [file1, file2, file3, temporaryFile]

        def uniques = []
        def duplicates = []

        inputList.each { uniques.contains(it) ? duplicates << it : uniques << it }

        // Test uniqueness:
        duplicates.size() == 0

        // Test format:
        [file1, file2, file3].each {
            assert it.canonicalPath != temporaryFile.canonicalPath
            assert it.canonicalPath.startsWith(temporaryFile.canonicalPath)
        }

    }

}
