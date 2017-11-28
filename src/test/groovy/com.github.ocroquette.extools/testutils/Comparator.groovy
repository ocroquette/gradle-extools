package com.github.ocroquette.extools.testutils

/**
 * Utility functions to compare structures likes maps or sets
 */
class Comparator {
    /**
     * Compare the given maps and return a string describing the differences, if any
     * @param reference the reference map
     * @param actual the actual map
     * @param excludedKeys the list of keys to ignore in both maps
     * @return a description of the differences, or the empty string if there are none
     */
    static String compareMaps(def reference, def actual, def excludedKeys) {
        String differences = ""

        Set<String> allKeys = []
        allKeys.addAll(reference.keySet())
        allKeys.addAll(actual.keySet())

        allKeys.each { key ->
            if (excludedKeys.contains(key))
                return
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
