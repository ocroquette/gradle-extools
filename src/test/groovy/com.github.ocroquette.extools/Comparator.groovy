package com.github.ocroquette.extools

class Comparator {
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

