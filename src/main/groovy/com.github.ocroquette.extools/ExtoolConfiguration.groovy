package com.github.ocroquette.extools

/**
 * Configuration of a single tool from the ".extool" file
 */
class ExtoolConfiguration {
    /**
     * Variables set in extool file with their value
     */
    private TreeMap<String, String> variables = [:]

    /**
     * Names of the variables to set as environment variables
     */
    private Set<String> variablesToSetInEnv = []

    /**
     * Names of the variables to prepend to the existing environment variables
     */
    private Set<String> variablesToPrependInEnv = []

}
