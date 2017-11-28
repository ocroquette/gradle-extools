package com.github.ocroquette.extools.tasks

import com.github.ocroquette.extools.internal.exec.Executor
import org.gradle.api.DefaultTask
import org.gradle.api.Task

class ExtoolExec extends DefaultTask {
    @Override
    Task configure(Closure configureClosure) {
        doLast {
            new Executor(project).executeConfiguration(configureClosure)
        }
    }
}
