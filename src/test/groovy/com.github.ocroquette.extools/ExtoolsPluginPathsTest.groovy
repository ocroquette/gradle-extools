package com.github.ocroquette.extools

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExtoolsPluginPathsTest extends Specification {
    @Rule
    final TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Rule
    final TemporaryFolder buildDirectory = new TemporaryFolder()

    @Rule
    final TemporaryFolder otherTmpDirectory = new TemporaryFolder()

    File buildFile
    def dumpFile
    def extractDir

    def setup() {
        buildFile = buildDirectory.newFile('build.gradle')
        def script = this.getClass().getResource('/build.gradle.paths').text

        buildFile << script
    }

    def "Default local cache must be in the user home"() {
        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                useDefaultLocalCache: true,
                taskName: 'printLocalCache',
        ).build()

        def expected = new File(System.properties["user.home"], ".gradle/extools/localCache").canonicalPath

        then:
        result.task(":printLocalCache").outcome == SUCCESS
        result.output.contains("localCache=" + expected)
    }

    def "Local cache can be set using a property"() {
        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                localCache: otherTmpDirectory.root.canonicalPath,
                taskName: 'printLocalCache',
        ).build()

        def expected = otherTmpDirectory.root.canonicalPath

        then:
        result.task(":printLocalCache").outcome == SUCCESS
        result.output.contains("localCache=" + expected)
    }

    def "Default extract dir must be in the user home"() {
        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                useDefaultExtractDir: true,
                taskName: 'printExtractDir',
        ).build()
        def expected = new File(System.properties["user.home"], ".gradle/extools/extractDir").canonicalPath

        then:
        result.task(":printExtractDir").outcome == SUCCESS
        result.output.contains("extractDir=" + expected)
    }

    def "Default extract dir can be set using a property"() {
        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                extractDir: otherTmpDirectory.root.canonicalPath,
                taskName: 'printExtractDir',
        ).build()
        def expected = otherTmpDirectory.root.canonicalPath

        then:
        result.task(":printExtractDir").outcome == SUCCESS
        result.output.contains("extractDir=" + expected)
    }

    def generateBuildScript() {
        return this.getClass().getResource('/build.gradle.paths').text
    }
}
