package com.github.ocroquette.extools

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskAction

class ExtoolsExec extends DefaultTask {
    def aliasesUsed = []

    @Override
    Task configure(Closure configureClosure) {
        doLast {
            Executor executor = new Executor(project)
            def c = executor.getExecClosure(configureClosure)
            c()
        }
    }
}
