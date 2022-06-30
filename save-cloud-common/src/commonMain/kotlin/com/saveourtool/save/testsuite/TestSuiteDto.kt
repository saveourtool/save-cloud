package com.saveourtool.save.testsuite

import com.saveourtool.save.entities.BaseDto
import com.saveourtool.save.entities.Project

import kotlinx.serialization.Serializable

/**
 * @property type [com.saveourtool.save.entities.TestSuite.type]
 * @property name [com.saveourtool.save.entities.TestSuite.name]
 * @property project [com.saveourtool.save.entities.TestSuite.project]
 * @property description [com.saveourtool.save.entities.TestSuite.description]
 * @property language
 * @property version
 * @property id
 * @property source
 */
@Serializable
data class TestSuiteDto(
    override val id: Long? = null,
    val type: TestSuiteType?,
    val name: String,
    val description: String?,
    val project: Project? = null,
    val source: TestSuitesSourceDto,
    val language: String? = null,
    val version: String
) : BaseDto() {
    /**
     * @return [com.saveourtool.save.entities.TestSuite.testRootPath]
     */
    @Deprecated("Need to remove usage of this variable from this entry")
    fun testRootPath() = GitLocation.parseFromDatabase(source.locationInfo).subDirectory

    /**
     * @return url of the repo with test suites
     */
    @Deprecated("Need to remove usage of this variable from this entry")
    fun testSuiteRepoUrl() = GitLocation.parseFromDatabase(source.locationInfo).httpUrl
}
