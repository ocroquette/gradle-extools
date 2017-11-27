package com.github.ocroquette.extools

import org.gradle.api.Project
import org.ysb33r.grolifant.api.exec.AbstractCommandExecSpec
import org.ysb33r.grolifant.api.exec.ResolverFactoryRegistry

class ExtoolsExecSpec extends AbstractCommandExecSpec {
    ExtoolsExecSpec(Project project) {
        super(project,new ResolverFactoryRegistry(project))
        println "In ExtoolsExecSpec"
        setExecutable('git')
    }
}