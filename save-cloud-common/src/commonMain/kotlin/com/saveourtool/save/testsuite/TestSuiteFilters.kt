package com.saveourtool.save.testsuite

import com.saveourtool.save.utils.DATABASE_DELIMITER
import kotlinx.serialization.Serializable

/**
 * Filters for test suites
 * @property name
 * @property language
 * @property tags
 */
@Serializable
data class TestSuiteFilters(
    val name: String,
    val language: String,
    val tags: String,
) {
    /**
     * @return [tags] as list
     */
    fun tagsAsList() = tags.split(DATABASE_DELIMITER).distinct()

    /**
     * @return [TestSuiteFilters] as query params for request
     */
    fun toQueryParams() = listOf("tags=" to tags, "name=" to name, "language" to language)
        .filter { it.second.isNotBlank() }
        .joinToString("&") { it.first + it.second }
        .let {
            if (it.isNotBlank()) {
                "?$it"
            } else {
                it
            }
        }

    /**
     * @return true if no filters are set, false otherwise
     */
    fun isEmpty() = name.isBlank() && tags.isBlank()

    companion object {
        val empty = TestSuiteFilters(name = "", tags = "", language = "")
    }
}
