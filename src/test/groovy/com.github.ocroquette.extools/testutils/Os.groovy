package com.github.ocroquette.extools.testutils

class Os {
    public static boolean isWindows() {
        return System.properties['os.name'].toLowerCase().contains('windows')
    }
}
