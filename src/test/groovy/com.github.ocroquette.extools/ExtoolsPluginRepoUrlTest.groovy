package com.github.ocroquette.extools

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class ExtoolsPluginRepoUrlTest extends Specification {
    static final REPO_DIR = "src/test/resources/extools/"
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
                repositoryUrl: new File(REPO_DIR).toURI().toURL(),
                buildScript: generateBuildScript(''),
                taskName: TASK_NAME,
        ).build()

        then:
        result.task(":${TASK_NAME}").outcome == SUCCESS
    }

    def "Build script must run when repo is provided in build script"() {
        when:
        def statement = "remoteRepositoryUrl '${new File(REPO_DIR).toURI().toURL()}'"
        def result = new GradleRunnerHelper(
                temporaryRoot: temporaryFolder.newFolder(),
                repositoryUrl: new File(REPO_DIR).toURI().toURL(),
                buildScript: generateBuildScript(statement),
                taskName: TASK_NAME,
        ).build()

        then:
        result.task(":${TASK_NAME}").outcome == SUCCESS
    }

    private String generateBuildScript(String remoteRepositoryUrlStatement) {
        return getScriptTemplate().text.replaceAll('%remoteRepositoryUrlStatement%', remoteRepositoryUrlStatement)

    }

    private URL getScriptTemplate() {
        this.getClass().getResource('/build.gradle.repourl')
    }

}
