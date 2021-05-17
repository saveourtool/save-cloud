package org.cqfn.save.testsuite

import org.cqfn.save.entities.Project

import kotlinx.serialization.Serializable

/**
 * @property type
 * @property name
 * @property project
 */
@Serializable
data class TestSuiteDto(
    val type: TestSuiteType,
    var name: String,
    var project: Project? = null,
)
