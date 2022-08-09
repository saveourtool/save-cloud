package com.saveourtool.save.entities

import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.DATABASE_DELIMITER

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property name name of the test suite
 * @property description description of the test suite
 * @property source source, which this test suite is created from
 * @property version version of source, which this test suite is created from
 * @property dateAdded date and time, when this test suite was added to the project
 * @property language
 * @property tags
 */
@Suppress("LongParameterList")
@Entity
class TestSuite(
    var name: String = "Undefined",

    var description: String? = "Undefined",

    @ManyToOne
    @JoinColumn(name = "source_id")
    var source: TestSuitesSource,

    var version: String,

    var dateAdded: LocalDateTime? = null,

    var language: String? = null,

    var tags: String? = null,
) : BaseEntity() {
    /**
     * @return [tags] as a list of strings
     */
    fun tagsAsList() = tags?.split(DATABASE_DELIMITER)?.filter { it.isNotBlank() }.orEmpty()

    /**
     * @param id
     * @return Dto of testSuite
     */
    fun toDto(id: Long? = null) =
            TestSuiteDto(
                this.name,
                this.description,
                this.source.toDto(),
                this.version,
                this.language,
                this.tagsAsList(),
            ).apply {
                this.id = id
            }

    companion object {
        /**
         * Concatenates [tags] using same format as [TestSuite.tagsAsList]
         *
         * @param tags list of tags
         * @return representation of [tags] as a single string understood by [TestSuite.tagsAsList]
         */
        fun tagsFromList(tags: List<String>) = tags.joinToString(separator = DATABASE_DELIMITER)
    }
}
