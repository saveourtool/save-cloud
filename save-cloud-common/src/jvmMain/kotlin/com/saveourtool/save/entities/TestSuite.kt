package com.saveourtool.save.entities

import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteType

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property type type of the test suite, one of [TestSuiteType]
 * @property name name of the test suite
 * @property description description of the test suite
 * @property project project, which this test suite belongs to
 * @property dateAdded date and time, when this test suite was added to the project
 * @property source [TestSuitesSource], which this test suite belongs to
 * @property language
 * @property version version of entry, git commit hash by default
 */
@Suppress("LongParameterList")
@Entity
class TestSuite(
    @Enumerated(EnumType.STRING)
    var type: TestSuiteType? = null,

    var name: String = "Undefined",

    var description: String? = "Undefined",

    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project? = null,

    var dateAdded: LocalDateTime? = null,

    @ManyToOne
    @JoinColumn(name = "source_id")
    var source: TestSuitesSource,

    var language: String? = null,

    var version: String
) : BaseEntity() {
    /**
     * @return Dto of testSuite
     */
    fun toDto() =
            TestSuiteDto(
                this.id,
                this.type,
                this.name,
                this.description,
                this.project,
                this.source.toDto(),
                this.language,
                this.version
            )

    /**
     * @return location of save.properties file for this test suite, relative to project's root directory
     */
    @Deprecated("Need to remove usage of this variable from this entry")
    fun testRootPath() = toDto().testRootPath()

    /**
     * @return url of the repo with test suites
     */
    @Deprecated("Need to remove usage of this variable from this entry")
    fun testSuiteRepoUrl() = toDto().testSuiteRepoUrl()
}
