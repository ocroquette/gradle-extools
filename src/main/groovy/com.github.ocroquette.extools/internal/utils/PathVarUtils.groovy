package com.github.ocroquette.extools.internal.utils

class PathVarUtils {
    static getSystemPathVariableName() {
        if ( Os.isWindows() ) {
            for (String name: System.getenv().keySet())
                if (name.toUpperCase() == "PATH")
                    return name
            return "Path"
        }
        else {
            return "PATH"
        }
    }
}
