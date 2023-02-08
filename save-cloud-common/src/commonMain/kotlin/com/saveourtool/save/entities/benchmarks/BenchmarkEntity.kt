/**
 * Common class for representing the entity used in awesome-benchamrks
 */

package com.saveourtool.save.entities.benchmarks

import com.saveourtool.save.frontend.TabMenuBar
import kotlinx.serialization.Serializable

@Serializable
@Suppress("WRONG_DECLARATIONS_ORDER")
/**
 * Enum that represents categories that could be used for grouping benchmarks (they should be selected for each benchmark).
 * This enum should be synchronized with enum in awesome-benchmarks
 */
enum class BenchmarkCategoryEnum {
    ALL,
    AI,
    AUDIT,
    CODING_STANDARD,
    PERFORMANCE,
    CODE_ANALYSIS,
    ;

    companion object : TabMenuBar<BenchmarkCategoryEnum> {
        // The string is the postfix of a [regexForUrlClassification] for parsing the url
        private val postfixInRegex = values().joinToString("|") { it.name.lowercase() }
        override val nameOfTheHeadUrlSection = "archive"
        override val defaultTab: BenchmarkCategoryEnum = ALL
        override val regexForUrlClassification: Regex = Regex("/$nameOfTheHeadUrlSection/[^/]+/($postfixInRegex)")
        override fun valueOf(elem: String): BenchmarkCategoryEnum = BenchmarkCategoryEnum.valueOf(elem)
        override fun values(): Array<BenchmarkCategoryEnum> = BenchmarkCategoryEnum.values()
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
