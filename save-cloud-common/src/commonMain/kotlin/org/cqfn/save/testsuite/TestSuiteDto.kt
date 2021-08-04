package org.cqfn.save.testsuite

import org.cqfn.save.entities.Project

import kotlinx.serialization.Serializable

/**
 * @property type [TestSuite.type]
 * @property name [TestSuite.name]
 * @property project [TestSuite.project]
 * @property propertiesRelativePath [TestSuite.propertiesRelativePath]
 */
@Serializable
data class TestSuiteDto(
    val type: TestSuiteType?,
    val name: String,
    val project: Project? = null,
    val propertiesRelativePath: String,
)
