package com.github.ocroquette.extools.internal.exec

/**
 * Provides the DSL to configure a call to an extool
 *
 * It mimics the DSL used by Gradle's standard Exec task and exec {} statement
 */
class ExecutionConfiguration {
    String executable
    List<String> args = []
    TreeMap<String, String> environment = System.getenv()
    OutputStream errorOutput = null
    Boolean ignoreExitValue = null
    InputStream standardInput = null
    OutputStream standardOutput = null
    File workingDir = null
    Map<String, String> prependEnvPath = [:]
    boolean runInBackground = false

    boolean usingExtoolsAppends = true
    List<String> usingExtools = []

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

    private stringify(Object o, String what) {
        // String, GString and File make sense, assume other types are a mistake:
        if (o instanceof String) {
            return o
        }
        else if (o instanceof GString) {
            return o.toString()
        }
        else if (o instanceof File) {
            return o.canonicalPath
        }
        else {
            throw new RuntimeException("Invalid type for $what: " + o.getClass().simpleName)
        }
    }

    def prependEnvPaths(def map) {
        map.each { name,  value ->
            def values = []
            if ( isCollectionOrArray(value) ) {
                values.addAll(value)
            }
            else {
                values.add(value)
            }
            def strings = values.collect { o ->
                stringify(o, "prependEnvPaths argument for \"$name\"")
            }
            prependEnvPath[name] = strings.join(File.pathSeparator)
        }
    }

    def args(Object... l) {
        this.args = []
        for (int i = 0 ; i < l.length ; i++) {
            Object o = l.getAt(i)
            this.args.push(stringify(o, "argument $i"))
        }
    }

    def environment(def map) {
        map.each { name,  value ->
            this.environment.put(name, stringify(value, "Value for $name"))
        }
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
        usingExtools = aliases as List<String>
        usingExtoolsAppends = false
    }

    void usingAdditionalExtools(String... aliases) {
        usingExtools.addAll(aliases)
        usingExtoolsAppends = true
    }

    void runInBackground(boolean b) {
        this.runInBackground = b
    }

    boolean runInBackground() {
        return this.runInBackground
    }

    void commandLine(Object... components) {
        this.executable(components[0])
        if (components.size() == 1)
            this.args = []
        else {
            Object[] slice = components[1..components.size() - 1]
            this.args(slice)
        }
    }

    private boolean isCollectionOrArray(object) {
        [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
    }
}
