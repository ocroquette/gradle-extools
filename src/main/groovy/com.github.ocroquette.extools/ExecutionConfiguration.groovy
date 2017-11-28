package com.github.ocroquette.extools

import org.gradle.api.Project

class ExecutionConfiguration {
    String executable
    List<String> args = []
    TreeMap<String, String> environment = System.getenv()
    OutputStream errorOutput = null
    Boolean ignoreExitValue = null
    InputStream standardInput = null
    OutputStream standardOutput = null
    File workingDir = null

    def usingExtools = []

    static ExecutionConfiguration fromClosure(Closure c) {
        ExecutionConfiguration conf = new ExecutionConfiguration()
        c.delegate = conf
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return conf
    }

    def executable(String s) {
        this.executable = s
    }

    def args(String... l) {
        this.args = l
    }

    def environment(def e) {
        this.environment = e
    }

    def errorOutput(OutputStream s) {
        this.errorOutput = s
    }

    def ignoreExitValue(boolean b) {
        this.ignoreExitValue = b
    }

    def standardInput(InputStream s) {
        this.standardInput = s
    }

    def standardOutput(OutputStream s) {
        this.standardOutput = s
    }

    def workingDir(File f) {
        this.workingDir = f
    }

    def workingDir(String s) {
        this.workingDir = new File(s)
    }

    void usingExtools(String... aliases) {
        usingExtools.addAll(aliases)
    }

    void commandLine(String... components) {
        this.executable = components[0]
        if (components.size() == 1)
            args = []
        else
            args = components[1..components.size() - 1]
    }
}
