package com.github.ocroquette.extools

import com.github.ocroquette.extools.testutils.Comparator
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import static org.gradle.testkit.runner.TaskOutcome.*

class ExtoolsPluginExtoolsExecTest extends Specification {

    static final REPO_DIR = "build/test/repo/"
    static final REPO_URL = new File(REPO_DIR).toURI().toURL().toString()

    @Rule
    final TemporaryFolder temporaryFolder = new TemporaryFolder()

    def dumpFile

    def setup() {
        dumpFile = temporaryFolder.newFile()
    }

    def "External tools dummy_1 is usable"() {
        given:
        def taskName = 'execDummy1'

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

    def "execExtool is working twice in a row"() {
        given:
        def taskName = 'execDummy1ThenDummy2'

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
        result.output.contains("Output from dummy 2")
    }

    def "External tools dummy_2 is usable"() {
        given:
        def taskName = 'execDummy2'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.contains("Output from dummy 2")
    }

    def "External tool in sub-directory is usable"() {
        given:
        def taskName = 'execToolInSubdir'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.contains("Output from dummy 3")
    }

    def "Tool is not found when not in PATH"() {
        given:
        def taskName = 'execPrintEnvVarsWithInvalidPath'

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

    def "PATH variable is extended as required"() {
        given:
        def taskName = 'execPrintEnvVarsWithImplicitPath'
        def extractDir = temporaryFolder.newFolder()
        def expectedEnv = getSysEnv()
        expectedEnv["PATH"] = new File(extractDir, "printenvvars/bin").canonicalPath + File.pathSeparator +
                new File(extractDir, "dummy_2/bin").canonicalPath + File.pathSeparator +
                getSystemPath()
        expectedEnv["PRINTENVVARS_BIN"] = new File(extractDir, "printenvvars/bin").canonicalPath
        expectedEnv["CMAKE_PREFIX_PATH"] = new File(extractDir, "dummy_2/cmake2").canonicalPath
        expectedEnv["DUMMY_STRING"] = "Value of DUMMY_STRING from dummy_2"
        expectedEnv["DUMMY2_STRING"] = "Value of DUMMY2_STRING"
        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                extractDir: extractDir.canonicalPath,
                taskName: taskName,
        ).build()
        def actualEnv = parseEnvVariablesFromStdout(dumpFile.text)

        then:
        result.task(":$taskName").outcome == SUCCESS
        compareEnv(expectedEnv, actualEnv) == ""
    }

    def "Extend environment variables from one tool"() {
        given:
        def taskName = 'execPrintEnvVarsWithEnvironmentVariableSingle'
        def extractDir = temporaryFolder.newFolder()
        def expectedEnv = getSysEnv()
        expectedEnv["PRINTENVVARS_BIN"] = new File(extractDir, "printenvvars/bin").canonicalPath
        expectedEnv["CMAKE_PREFIX_PATH"] = new File(extractDir, "dummy_2/cmake2").canonicalPath
        expectedEnv["DUMMY_STRING"] = "Value of DUMMY_STRING from dummy_2"
        expectedEnv["DUMMY2_STRING"] = "Value of DUMMY2_STRING"
        expectedEnv["PATH"] =
                new File(extractDir, "printenvvars/bin").canonicalPath + File.pathSeparator +
                        new File(extractDir, "dummy_2/bin").canonicalPath + File.pathSeparator +
                        getSystemPath()

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                extractDir: extractDir.canonicalPath,
                taskName: taskName,
        ).build()
        def actualEnv = parseEnvVariablesFromStdout(dumpFile.text)

        then:
        result.task(":$taskName").outcome == SUCCESS
        compareEnv(expectedEnv, actualEnv) == ""
    }

    def "Extend environment variables from all tools"() {
        given:
        def taskName = 'execUseAll'

        def extractDir = temporaryFolder.newFolder()

        def expectedEnv = getSysEnv()
        expectedEnv["PRINTENVVARS_BIN"] = new File(extractDir, "printenvvars/bin").canonicalPath
        expectedEnv["CMAKE_PREFIX_PATH"] =
                new File(extractDir, "dummy_1/cmake_").canonicalPath + File.pathSeparator +
                        new File(extractDir, "dummy_1/cmake").canonicalPath + File.pathSeparator +
                        new File(extractDir, "dummy_2/cmake2").canonicalPath
        expectedEnv["DUMMY_STRING"] = "Value of DUMMY_STRING from dummy_2"
        expectedEnv["DUMMY1_STRING"] = "Value of DUMMY1_STRING"
        expectedEnv["DUMMY2_STRING"] = "Value of DUMMY2_STRING"
        expectedEnv["PATH"] = // Order must match the one used in the usingExtools statement:
                new File(extractDir, "dummy_1/bin").canonicalPath + File.pathSeparator +
                        new File(extractDir, "printenvvars/bin").canonicalPath + File.pathSeparator +
                        new File(extractDir, "printargs/bin").canonicalPath + File.pathSeparator +
                        new File(extractDir, "subdir/dummy_3/bin").canonicalPath + File.pathSeparator +
                        new File(extractDir, "dummy_2/bin").canonicalPath + File.pathSeparator +
                        getSystemPath()
        expectedEnv["DUMMY1_DIR"] = new File(extractDir, "dummy_1/").canonicalPath

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                extractDir: extractDir.canonicalPath,
                taskName: taskName,
        ).build()
        def actualEnv = parseEnvVariablesFromStdout(dumpFile.text)

        then:
        result.task(":$taskName").outcome == SUCCESS
        compareEnv(expectedEnv, actualEnv) == ""
    }

    def "Execute system command"() {
        given:
        def taskName = 'execSystemCommand'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
    }

    def "Pass Files as arguments"() {
        given:
        def taskName = 'passFileAsArgument'

        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()

        then:
        result.task(":$taskName").outcome == SUCCESS
        result.output.matches("(?s).*ARG1=\"?a_file\"?.*")
    }

    def parseEnvVariablesFromStdout(String stdout) {
        def variables = [:]
        stdout.eachLine { line ->
            def fields = line.split("=", 2)
            variables[fields[0]] = fields[1]
        }
        return variables
    }

    def compareEnv(def reference, def actual) {
        Set<String> excludedKeys = []
        excludedKeys.addAll(
                [
                        // Special Unix variables
                        "SHLVL",
                        "_",
                        "PWD",
                        "OLDPWD",
                        "XPC_SERVICE_NAME", // Not sure what this one is about, sample value: "com.jetbrains.intellij.ce.13020"
                        // Special Windows variables
                        "PROMPT",
                        "=::"
                ]
        )
        return Comparator.compareEnvs(reference, actual, excludedKeys)
    }

    private String generateBuildScript() {
        String template = this.getClass().getResource('/build.gradle.extoolsexec').text
        String dumpFileEscaped = org.apache.commons.text.StringEscapeUtils.escapeJava(dumpFile.canonicalPath)
        String result = template.replace('%dumpFile%', dumpFileEscaped)
        return result
    }

    private getSysEnv() {
        def envCopy = [:]
        envCopy.putAll(System.getenv())
        return envCopy
    }

    private getSystemPath() {
        return trimPath(System.getenv("PATH"))
    }

    // Extools removes any leading or trailing separators
    private trimPath(String s) {
        String sep = File.pathSeparator
        s = s.replaceAll("^$sep+", "")
        s = s.replaceAll("$sep+\$", "")
        return s
    }
}
