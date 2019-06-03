package com.github.ocroquette.extools.testutils

import com.github.ocroquette.extools.internal.utils.Os

/**
 * Utility functions to compare structures likes maps or sets
 */
class Comparator {
    private static convertToKeysToLowercase(def map) {
        def newMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER)
        newMap.putAll(map)
        return newMap
    }

    /**
     * Compare the given environments and return a string describing the differences, if any
     * @param reference the reference map
     * @param actual the actual map
     * @param excludedKeys the list of keys to ignore in both maps
     * @param ignoreKeyCase ignore the case of the map keys
     * @return a description of the differences, or the empty string if there are none
     */
    static String compareEnvs(def reference, def actual, def excludedKeys) {
        String differences = ""

        if (Os.isWindows()) {
            // Ignore case of variable names
            reference = convertToKeysToLowercase(reference)
            actual = convertToKeysToLowercase(actual)
        }

        Set<String> allKeys = []
        allKeys.addAll(reference.keySet())
        allKeys.addAll(actual.keySet())



        def isWindows = System.getProperty("os.name").toLowerCase().contains("windows")

        allKeys.each { key ->
            if (excludedKeys.contains(key))
                return

            if (isWindows && key.startsWith("=")) {
                // Windows add special variables starting with a "=". They are hidden by the standard Windows tools,
                // but not by Java. In any case, they are not relevant for us, and can cause the tests to fail, so
                // just ignore them
                return
            }

            String referenceValueAsString = (reference[key] == null ? "(null)" : "\"${reference[key]}\"")
            String actualValueAsString = (actual[key] == null ? "(null)" : "\"${actual[key]}\"")

            if (referenceValueAsString != actualValueAsString) {
                differences += "For variable \"$key\":\n"
                differences += "  Reference: $referenceValueAsString\n"
                differences += "  Actual:    $actualValueAsString\n"
            }
        }

        return differences
    }

    /**
     * Compare the given collections as sets and return a string describing the differences, if any
     * @param reference the reference collection
     * @param actual the actual collection
     * @return a description of the differences, or the empty string if there are none
     */
    static String compareAsSets(def reference, def actual) {
        def unexpected = []
        def missing = []
        for(String ref: reference) {
            if ( ! actual.contains(ref) )
                missing.add(ref)
        }
        for(String act: actual) {
            if ( ! reference.contains(act) )
                unexpected.add(act)
        }

        String differences = ""
        if ( unexpected.size() > 0 )
            differences += "Unexpected: " + unexpected.join(",") + "\n"

        if ( missing.size() > 0 )
            differences += "Missing: " + missing.join(",")

        return differences
    }
}

