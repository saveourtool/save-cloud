/**
 * Common class for representing the entity used in awesome-benchamrks
 */

package com.saveourtool.save.entities.benchmarks

import com.saveourtool.save.domain.Role
import kotlinx.serialization.Serializable

/**
 * Interface for tab bar in many pages
 */
interface MenuBar<T> {
    /**
     * Default value in every Enum classes
     */
    val defaultTab: T

    /**
     * Regular expression for url classification
     */
    val regex: Regex

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
    fun findEnumElements(elem: String): T?

    /**
     * Function set shortPath and longPath in Pair path
     *
     * @param shortPath
     * @param longPath
     */
    fun setPath(shortPath: String, longPath: String)

    /**
     * @param elem
     * @return the string of the entered Enum class element
     */
    fun returnStringOneOfElements(elem: T): String

    /**
     * @param role
     * @param elem
     * @param flag
     * @return Returns true if the check for this tab and role is not passed, else return false
     */
    fun isNotAvailableWithThisRole(role: Role, elem: T?, flag: Boolean?): Boolean
}

@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
/**
 * Enum that represents categories that could be used for grouping benchmarks (they should be selected for each benchmark)
 */
enum class BenchmarkCategoryEnum {
    ALL,
    AI,
    AUDIT,
    CODING_STANDARD,
    PERFORMANCE,
    STATIC_ANALYSIS,
    ;

    companion object : MenuBar<BenchmarkCategoryEnum> {
        override val defaultTab = ALL
        val listOfStringEnumElements = BenchmarkCategoryEnum.values().map { it.name.lowercase() }
        override val regex = Regex("/project/[^/]+/[^/]+/[^/]+")
        override var paths: Pair<String, String> = "" to ""
        override fun valueOf(elem: String): BenchmarkCategoryEnum = BenchmarkCategoryEnum.valueOf(elem)
        override fun values(): Array<BenchmarkCategoryEnum> = BenchmarkCategoryEnum.values()
        override fun findEnumElements(elem: String): BenchmarkCategoryEnum? = values().find { it.name.lowercase() == elem }
        override fun setPath(shortPath: String, longPath: String) {
            paths = shortPath to longPath
        }

        override fun returnStringOneOfElements(elem: BenchmarkCategoryEnum): String = elem.name

        override fun isNotAvailableWithThisRole(role: Role, elem: BenchmarkCategoryEnum?, flag: Boolean?): Boolean = false
    }
}

/**
 * @property general
 * @property info
 * @property links
 */
@Serializable
data class BenchmarkEntity(
    val general: General,
    val info: Info,
    val links: Links,
)

/**
 * @property name
 * @property category
 * @property tags
 */
@Serializable
data class General(
    val name: String,
    val category: BenchmarkCategoryEnum,
    val tags: ArrayList<String>,
)

/**
 * @property language
 * @property license
 * @property scenarios_num
 * @property description
 */
@Serializable
data class Info(
    val language: String,
    val license: String,
    @Suppress("VARIABLE_NAME_INCORRECT_FORMAT", "ConstructorParameterNaming")
    val scenarios_num: Long,
    val description: String,
)

/**
 * @property homepage
 * @property sources
 * @property documentation
 */
@Serializable
data class Links(
    val homepage: String,
    val sources: String,
    val documentation: String,
)
