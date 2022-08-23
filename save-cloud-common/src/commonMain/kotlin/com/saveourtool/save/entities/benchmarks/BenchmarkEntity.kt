/**
 * Common class for representing the entity used in awesome-benchamrks
 */

package com.saveourtool.save.entities.benchmarks

import com.saveourtool.save.domain.Role
import kotlinx.serialization.Serializable


interface MenuBar<T> {
    val defaultTab: T
    val regex: Regex
    fun values(): Array<T>
    fun valueOf(): T

    fun findEnumElements(elem: String): T?

    var paths: Pair<String, String>

    fun setPath(shortPath: String, longPath: String)

    fun returnStringOneOfElements(elem: T) : String

    fun isAvailableWithThisRole(role: Role, elem: T?, flag: Boolean?) : Boolean
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

    companion object: MenuBar<BenchmarkCategoryEnum> {
        override fun valueOf(): BenchmarkCategoryEnum = BenchmarkCategoryEnum.valueOf()
        override fun values(): Array<BenchmarkCategoryEnum> = BenchmarkCategoryEnum.values()
        override val defaultTab = ALL
        val listOfStringEnumElements = BenchmarkCategoryEnum.values().map { it.name.lowercase() }
        override val regex = Regex("/project/[^/]+/[^/]+/[^/]+")
        override fun findEnumElements(elem: String): BenchmarkCategoryEnum? = values().find { it.name.lowercase() == elem }

        override var paths: Pair<String, String> = "" to ""
        override fun setPath(shortPath: String, longPath: String) {
            paths = shortPath to longPath
        }

        override fun returnStringOneOfElements(elem: BenchmarkCategoryEnum) : String = elem.name

        override fun isAvailableWithThisRole(role: Role, elem: BenchmarkCategoryEnum?, flag: Boolean?): Boolean = true
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
