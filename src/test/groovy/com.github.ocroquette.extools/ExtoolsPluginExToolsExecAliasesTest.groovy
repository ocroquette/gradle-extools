package com.github.ocroquette.extools

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExtoolsPluginExToolsExecAliasesTest extends Specification {
    static final REPO_DIR = "build/test/repo/"
    static final REPO_URL = new File(REPO_DIR).toURI().toURL().toString()

    @Rule
    final TemporaryFolder temporaryFolder = new TemporaryFolder()

    def "Real names must not work when using aliases"() {
        given:
        def taskName = 'execDummy1'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).buildAndFail()

        then:
        result.task(":$taskName").outcome == FAILED
        result.output.contains("Use alias \"alias_1\" instead of real name \"dummy_1\"")
    }

    def "External tool dummy_1 is usable as alias_1"() {
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

    def "Access existing variable with getValue"() {
        given:
        def taskName = 'getValueExisting'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.find("(?m)^Value of PATH for alias_1: .*bin2" + File.pathSeparator + ".*bin\$")
    }

    def "Access undefined variable with getValue"() {
        given:
        def taskName = 'getValueMissing'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).buildAndFail()

        then:
        result.task(":$taskName").outcome == FAILED
    }

    def "Access existing variable with getValueWithDefault"() {
        given:
        def taskName = 'getValueWithDefaultExisting'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.find("(?m)^Value of PATH for alias_1: .*bin2" + File.pathSeparator + ".*bin\$")
    }

    def "Access undefined variable with getValueWithDefault"() {
        given:
        def taskName = 'getValueWithDefaultMissing'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.find("(?m)^Value of DUMMY1_VAR_MISSING for alias_1: Default value for DUMMY1_VAR_MISSING\$")
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
        result.output.contains("alias_1_home=" + (new File(extractDir, "dummy_1").canonicalPath))
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
        result.output.find("(?m)^alias_1=dummy_1\$")
    }

    def "Get loaded aliases"() {
        given:
        def taskName = 'getLoadedAliases'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.find("(?m)^aliases=alias_1,alias_2,alias_3\$")
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

    def "Generate environment script"() {
        when:
        def taskName = "generateEnvironmentScript"
        def extractDir = temporaryFolder.newFolder()
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                extractDir: extractDir,
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()
        String expected
        if (System.properties['os.name'].toLowerCase().contains('windows')) {
            expected = "set DUMMY2_STRING=Value of DUMMY2_STRING\n" +
                    "set DUMMY_STRING=Value of DUMMY_STRING from dummy_2\n" +
                    "set CMAKE_PREFIX_PATH=" + new File(extractDir, "dummy_2/cmake2").canonicalPath + ";%CMAKE_PREFIX_PATH%\n" +
                    "set PATH=" + new File(extractDir, "dummy_2/bin").canonicalPath + ";%PATH%\n"
        } else {
            expected = "export DUMMY2_STRING=Value of DUMMY2_STRING\n" +
                    "export DUMMY_STRING=Value of DUMMY_STRING from dummy_2\n" +
                    "export CMAKE_PREFIX_PATH=" + new File(extractDir, "dummy_2/cmake2").canonicalPath + ":\$CMAKE_PREFIX_PATH\n" +
                    "export PATH=" + new File(extractDir, "dummy_2/bin").canonicalPath + ":\$PATH\n"
        }
        then:
        result.task(":" + taskName).outcome == SUCCESS
        result.output.contains("generatedScript=" + expected + "\n" + "/generatedScript")
    }

    private String generateBuildScript() {
        this.getClass().getResource('/build.gradle.extoolsaliases').text
    }
}
