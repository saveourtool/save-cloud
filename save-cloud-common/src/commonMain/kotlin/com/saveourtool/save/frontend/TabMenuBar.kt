package com.saveourtool.save.frontend

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
     * Contains the url of the default tab
     */
    var pathDefaultTab: String

    /**
     * Contains the prefix of the url of the tabs
     */
    var extendedViewPath: String

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
    fun valueOfOrNull(elem: String): T? = values().firstOrNull { valueOf(elem.uppercase()) == it }

    /**
     * Function set shortPath and longPath in Pair path
     *
     * @param shortPath
     * @param longPath
     */
    fun setPath(shortPath: String, longPath: String) {
        pathDefaultTab = shortPath
        extendedViewPath = longPath
    }

    /**
     * @param role
     * @param elem
     * @param isOrganizationCanCreateContest
     * @return Returns true if the check for this tab and role is not passed, else return false
     */
    fun isNotAvailableWithThisRole(role: Role, elem: T?, isOrganizationCanCreateContest: Boolean?): Boolean
}
