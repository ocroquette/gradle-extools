package com.github.ocroquette.extools

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExtoolsPluginExToolsExecAliasesTest extends Specification {
    static final REPO_DIR = "build/test/repo/"
    static final REPO_URL = new File(REPO_DIR).toURI().toURL().toString()

    @Rule
    final TemporaryFolder temporaryFolder = new TemporaryFolder()

    def "External tool dummy_1 is usable with as alias_1"() {
        given:
        def taskName = 'execAlias1'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
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
                repositoryUrl: REPO_URL,
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
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.contains("Value of DUMMY1_VAR")
    }

    def "Access home dir"() {
        given:
        def taskName = 'accessHomeDir'
        def extractDir = temporaryFolder.newFolder()

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
                useDefaultExtractDir: false,
                extractDir: extractDir
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.contains("alias_1_home=" + ( new File(extractDir, "dummy_1").canonicalPath) )
    }

    def "Resolve alias"() {
        given:
        def taskName = 'resolveAlias'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.contains("alias_1=dummy_1")
    }


    def "Show information about extools"() {
        given:
        def taskName = 'extoolsInfo'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()
        println result.output

        then:
        result.task(":$taskName").outcome == SUCCESS
        // Normalize all strings, otherwise strings may not match due do different line endings
        result.output.normalize().matches("""(?s).*
globalconfig:
  repositoryUrl: .+
  localCache: .+
  extractDir: .+
tools:
  -
    alias: alias_1
    realname: dummy_1
    variables:
      CMAKE_PREFIX_PATH: .+
      DUMMY1_DIR: .+
      DUMMY1_STRING: Value of DUMMY1_STRING
      DUMMY1_VAR: Value of DUMMY1_VAR
      DUMMY_STRING: Value of DUMMY_STRING from dummy_1
      PATH: .+
    variablesToSetInEnv:
      - DUMMY1_DIR
      - DUMMY1_STRING
      - DUMMY_STRING
    variablesToPrependInEnv:
      - CMAKE_PREFIX_PATH
      - PATH
  -
    alias: alias_2
    realname: dummy_1
    variables:
      CMAKE_PREFIX_PATH: .+
      DUMMY1_DIR: .+
      DUMMY1_STRING: Value of DUMMY1_STRING
      DUMMY1_VAR: Value of DUMMY1_VAR
      DUMMY_STRING: Value of DUMMY_STRING from dummy_1
      PATH: .+
    variablesToSetInEnv:
      - DUMMY1_DIR
      - DUMMY1_STRING
      - DUMMY_STRING
    variablesToPrependInEnv:
      - CMAKE_PREFIX_PATH
      - PATH
.*""".normalize())
    }


    private String generateBuildScript() {
        this.getClass().getResource('/build.gradle.extoolsaliases').text
    }
}
