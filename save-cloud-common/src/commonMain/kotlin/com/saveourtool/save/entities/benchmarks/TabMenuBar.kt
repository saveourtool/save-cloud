package com.saveourtool.save.entities.benchmarks

import com.saveourtool.save.domain.Role

/**
 * Interface for tab bar in many pages
 */
interface TabMenuBar<T> {
    /**
     * Default value in every Enum classes
     */
    val defaultTab: T

    /**
     * Regular expression for url classification
     */
    val regexForUrlClassification: Regex

    /**
     * Pair consisting of a short address of the default tab and a long prefix of the addresses of all other tabs
     */
    var paths: Pair<String, String>

    /**
     * @return Array of elements this Enum
     */
    fun values(): Array<T>

    /**
     * @param elem
     * @return The Enum element by the string it corresponds to, or an exception if it is not found
     */
    fun valueOf(elem: String): T

    /**
     * @param elem
     * @return Equivalent to valueOf(), but returns null instead of an exception
     */
    fun findEnumElement(elem: String): T?

    /**
     * Function set shortPath and longPath in Pair path
     *
     * @param shortPath
     * @param longPath
     */
    fun setPath(shortPath: String, longPath: String) {
        paths = shortPath to longPath
    }

    /**
     * @param elem
     * @return the string of the entered Enum class element
     */
    fun convertEnumElemToString(elem: T): String

    /**
     * @param role
     * @param elem
     * @param flag
     * @return Returns true if the check for this tab and role is not passed, else return false
     */
    fun isNotAvailableWithThisRole(role: Role, elem: T?, flag: Boolean?): Boolean
}
