package org.cqfn.save.testsuite

import org.cqfn.save.entities.Project

import kotlinx.serialization.Serializable

/**
 * @property type [TestSuite.type]
 * @property name [TestSuite.name]
 * @property project [TestSuite.project]
 * @property testRootPath [TestSuite.testRootPath]
 * @property testSuiteRepoUrl url of the repo with test suites
 * @property description [TestSuite.description]
 */
@Serializable
data class TestSuiteDto(
    val type: TestSuiteType?,
    val name: String,
    val description: String?,
    val project: Project? = null,
    val testRootPath: String,
    val testSuiteRepoUrl: String? = null,
)
