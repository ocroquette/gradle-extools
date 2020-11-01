package com.github.ocroquette.extools

import com.github.ocroquette.extools.testutils.Comparator
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.apache.commons.text.StringEscapeUtils.escapeJava
import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExtoolsPluginExtoolLauncherTest extends Specification {

    static final REPO_DIR = "build/test/repo/"
    static final REPO_URL = new File(REPO_DIR).toURI().toURL().toString()

    @Rule
    final TemporaryFolder temporaryFolder = new TemporaryFolder()

    def dumpFile
    def tempFile1
    def tempFile2

    boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
    def scriptExtension = ( isWindows ? ".bat" : ".sh" )

    def setup() {
        dumpFile = temporaryFolder.newFile()
        tempFile1 = temporaryFolder.newFile()
        // tempFile2 has already the extension and is in a non existing folder
        // - the extension must not be added automatically by extoollauncher
        // - the directory must be created automatically
        tempFile2 = new File(temporaryFolder.newFolder(), "non_existing_folder/tempFile2" + scriptExtension)
    }

    def "Simple launcher 1"() {
        given:
        def taskName = 'generateLauncher1'

        when:
        def extractDir = temporaryFolder.newFolder()
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                extractDir: extractDir,
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()
        def actual_lines = new File(tempFile1.path + scriptExtension).text.split("\n")
        def expected_lines
        def dummy_2_dir = new File(extractDir, "dummy_2").canonicalPath
        if(isWindows) {
            expected_lines = ['set DUMMY2_STRING=Value of DUMMY2_STRING',
                              'set DUMMY_STRING=Value of DUMMY_STRING from dummy_2',
                              'set CMAKE_PREFIX_PATH=' + dummy_2_dir + '\\cmake2;%CMAKE_PREFIX_PATH%',
                              'set PATH=' + dummy_2_dir + '\\bin;%PATH%',
            ]
        }
        else {
            expected_lines = ['export DUMMY2_STRING="Value of DUMMY2_STRING"',
                              'export DUMMY_STRING="Value of DUMMY_STRING from dummy_2"',
                              'export CMAKE_PREFIX_PATH="' + dummy_2_dir + '/cmake2:$CMAKE_PREFIX_PATH"',
                              'export PATH="' + dummy_2_dir + '/bin:$PATH"',
            ]
        }

        then:
        result.task(":$taskName").outcome == SUCCESS
        Comparator.compareAsSets(expected_lines, actual_lines) == ""
    }

    def "Launcher with payload"() {
        given:
        def taskName = 'generateLauncher2'

        when:
        def extractDir = temporaryFolder.newFolder()
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                extractDir: extractDir,
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()
        def actual_lines = tempFile2.text.split(/\r?\n/)
        def expected_lines = ['Line 1', 'Line 2$\\']

        then:
        result.task(":$taskName").outcome == SUCCESS
        Comparator.compareAsSets(expected_lines, actual_lines) == ""
    }

    def "Launcher with additional environment"() {
        given:
        def taskName = 'generateLauncherWithAdditionalEnvironment'

        when:
        def extractDir = temporaryFolder.newFolder()
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(),
                extractDir: extractDir,
                repositoryUrl: REPO_URL,
                taskName: taskName,
        ).build()
        def actual_lines = new File(tempFile1.path + scriptExtension).text.split(/\r?\n/)
        def dummy_3_dir = new File(extractDir, "subdir/dummy_3").canonicalPath
        def expected_lines
        if(isWindows) {
            expected_lines = ['set PATH=' + dummy_3_dir + '\\bin;%PATH%',
                              "set VAR1=VALUE1",
                              "set VAR2=VALUE2",
                              "Hello",
                              "world"
            ]
        }
        else {
            expected_lines = ['export PATH="' + dummy_3_dir + '/bin:$PATH"',
                              "export VAR1=\"VALUE1\"",
                              "export VAR2=\"VALUE2\"",
                              "Hello",
                              "world"
            ]
        }

        then:
        result.task(":$taskName").outcome == SUCCESS
        Comparator.compareAsSets(expected_lines, actual_lines) == ""
    }

    private String generateBuildScript() {
        String template = this.getClass().getResource('/build.gradle.extoollauncher').text
        return template.
                replace('%dumpFile%', escapeJava(dumpFile.canonicalPath)).
                replace('%tempFile1%', escapeJava(tempFile1.canonicalPath)).
                replace('%tempFile2%', escapeJava(tempFile2.canonicalPath))
    }
}
