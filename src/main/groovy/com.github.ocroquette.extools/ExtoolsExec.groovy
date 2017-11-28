package com.github.ocroquette.extools

import org.gradle.api.DefaultTask
import org.gradle.api.Task

class ExtoolsExec extends DefaultTask {
    @Override
    Task configure(Closure configureClosure) {
        doLast {
            new Executor(project).executeConfiguration(configureClosure)
        }
    }
}
