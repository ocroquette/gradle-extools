package com.github.ocroquette.extools.tasks

import com.github.ocroquette.extools.internal.exec.ExecutionConfiguration
import com.github.ocroquette.extools.internal.exec.Executor
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Task type used when extoolExec is called interactively from the command line.
 *
 * gradlew extoolExec --commandLine=... --usingExtools=...
 */
class ExtoolExecCli extends DefaultTask {
    @Input
    String inputCommandLine = ""

    @Input
    String inputUsingExtools = ""

    @Option(option = 'commandLine', description = 'Set the command line to be executed (mandatory)')
    void setCommandLine(final String commandLine) {
        this.inputCommandLine = commandLine
    }

    @Option(option = 'usingExtools', description = 'Set the extools to use, comma separated (optional)')
    void setUsingExtools(final String usingExtools) {
        this.inputUsingExtools = usingExtools
    }

    @TaskAction
    def run() {
        if (inputCommandLine.isEmpty())
            throw new RuntimeException("Please provide a command line using --commandLine")

        new Executor(project).executeConfiguration({ ->
            commandLine inputCommandLine.split("\\s+")
            if (!inputUsingExtools.isEmpty()) {
                usingExtools inputUsingExtools.split(",")
            }
        })
    }
}
