package com.github.ocroquette.extools

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExtoolsPluginRepoUrlTest extends Specification {
    static final REPO_DIR = "build/test/repo/"

    public static final String TASK_NAME = 'execDummy1'

    @Rule
    final TemporaryFolder temporaryFolder = new TemporaryFolder()

    def "Build script must fail when no repo is provided"() {
        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                buildScript: generateBuildScript(''),
                taskName: TASK_NAME,
        ).buildAndFail()

        then:
        result.task(":${TASK_NAME}") == null
    }

    def "Build script must run when repo is provided as property"() {
        when:
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                repositoryUrl: getAsUrlString(REPO_DIR),
                buildScript: generateBuildScript(''),
                taskName: TASK_NAME,
        ).build()

        then:
        result.task(":${TASK_NAME}").outcome == SUCCESS
    }

    def "Build script must run when repo is provided in build script"() {
        when:
        def statement = "remoteRepositoryUrl '${getAsUrlString(REPO_DIR)}'"
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                repositoryUrl: getAsUrlString(REPO_DIR),
                buildScript: generateBuildScript(statement),
                taskName: TASK_NAME,
        ).build()

        then:
        result.task(":${TASK_NAME}").outcome == SUCCESS
    }

    private String generateBuildScript(String remoteRepositoryUrlStatement) {
        return getScriptTemplate().text.replaceAll('%remoteRepositoryUrlStatement%', remoteRepositoryUrlStatement)

    }

    private String getAsUrlString(String directory) {
        String canonicalPath = new File(directory).canonicalPath
        String string = new File(canonicalPath).toURI().toURL().toString()
        // Strip trailing (back)slashes, we want it to work without
        return string.replaceAll('[/\\\\/]+$', "")
    }

    private URL getScriptTemplate() {
        this.getClass().getResource('/build.gradle.repourl')
    }

}
