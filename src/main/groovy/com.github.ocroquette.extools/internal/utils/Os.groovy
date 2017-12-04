package com.github.ocroquette.extools.internal.utils

class Os {
    static boolean isWindows() {
        return ( System.getProperty("os.name").toLowerCase().contains("windows") ? true : false )
    }
}
