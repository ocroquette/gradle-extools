package com.github.ocroquette.extools

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExtoolsPluginExToolsExecAliasesTest extends Specification {
    static final REPO_DIR = "build/test/repo/"

    @Rule
    final TemporaryFolder temporaryFolder = new TemporaryFolder()

    def "External tool dummy_1 is usable with as alias_1"() {
        given:
        def taskName = 'execAlias1'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: new File(REPO_DIR).toURI().toURL().toString(),
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.contains("Output from dummy 1")
    }

    def "External tool dummy_1 is usable with as alias_2"() {
        given:
        def taskName = 'execAlias2'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: new File(REPO_DIR).toURI().toURL().toString(),
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.contains("Output from dummy 1")
    }

    def "Access variable"() {
        given:
        def taskName = 'accessVariable'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: new File(REPO_DIR).toURI().toURL().toString(),
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.contains("Value of DUMMY1_VAR")
    }


    private String generateBuildScript() {
        this.getClass().getResource('/build.gradle.extoolsaliases').text
    }
}
