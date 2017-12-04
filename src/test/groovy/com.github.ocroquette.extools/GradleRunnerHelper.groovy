package com.github.ocroquette.extools

import org.gradle.testkit.runner.GradleRunner

class GradleRunnerHelper {
    File temporaryRoot
    String taskName
    String repositoryUrl

    boolean useDefaultLocalCache = false
    String localCache

    boolean useDefaultExtractDir = false
    String extractDir

    String buildScript
    List<String> additionalArguments = []

    def build() {
        return createRunner().build()
    }

    def buildAndFail() {
        return createRunner().buildAndFail()
    }

    GradleRunner createRunner() {

        List<String> arguments = [taskName, '--stacktrace']
        arguments.addAll(additionalArguments)

        def buildDir = new File(temporaryRoot, "buildDir")
        buildDir.mkdirs()

        new File(buildDir, "build.gradle").text = buildScript

        new File(buildDir, "gradle.properties").text = generateGradleProperties()

        return GradleRunner.create()
                .withProjectDir(buildDir)
                .withArguments(arguments)
                .withPluginClasspath()
    }

    private String generateGradleProperties() {
        Properties gradleProperties = new Properties()

        if ( repositoryUrl != null )
            gradleProperties.setProperty('extools.repositoryUrl', repositoryUrl)

        if ( ! useDefaultLocalCache ) {
            if (localCache != null)
                gradleProperties.setProperty('extools.localCache', localCache)
            else
                gradleProperties.setProperty('extools.localCache', new File(temporaryRoot, "localCache").canonicalPath)
        }

        if ( ! useDefaultExtractDir ) {
            if (extractDir != null)
                gradleProperties.setProperty('extools.extractDir', extractDir)
            else
                gradleProperties.setProperty('extools.extractDir', new File(temporaryRoot, "extractDir").canonicalPath)
        }
        StringWriter writer = new StringWriter()
        gradleProperties.store(writer, null)
        return writer.getBuffer().toString()
    }
}
