package com.github.ocroquette.extools.internal.exec

/**
 * Provides the DSL to configure a launcher
 */
class LauncherConfiguration {
    File launcherFile = null
    boolean usingExtoolsAppends = true
    List<String> usingExtools = []
    String textAfter = ""
    String textBefore = ""
    TreeMap<String, String> additionalEnvironment = []

    static LauncherConfiguration fromClosure(Closure c) {
        LauncherConfiguration conf = new LauncherConfiguration()
        c.delegate = conf
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        return conf
    }

    def launcherFile(File f) {
        this.launcherFile = f
    }

    def launcherFile(String s) {
        this.launcherFile = new File(s)
    }

    private def normalizeText(String s) {
        s = s.replace("\r\n", "\n")
                .replace("\r", "\n");
        if ( System.getProperty("os.name").toLowerCase().contains("windows") ) {
            s = s.replace("\n", System.lineSeparator())
        }
        return s
    }

    def textAfter(String s) {
        this.textAfter = normalizeText(s)
    }

    def textBefore(String s) {
        this.textBefore = normalizeText(s)
    }

    def usingExtools(String... aliases) {
        usingExtools = aliases as List<String>
        usingExtoolsAppends = false
    }

    def usingAdditionalExtools(String... aliases) {
        usingExtools.addAll(aliases)
        usingExtoolsAppends = true
    }

    def additionalEnvironment(env) {
        this.additionalEnvironment = env
    }
}
