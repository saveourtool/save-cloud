package org.cqfn.save.testsuite

import kotlinx.serialization.Serializable
import org.cqfn.save.entities.Project

/**
 * @property type
 * @property name
 * @property project
 */
@Serializable
data class TestSuiteDto(
    var type: TestSuiteType,
    var name: String,
    var project: Project? = null,
)
