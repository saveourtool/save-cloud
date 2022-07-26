package com.saveourtool.save.testsuite

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Organization
import kotlinx.serialization.Serializable

/**
 * @param organizationName
 * @param name
 * @param description
 * @param gitDto
 * @param branch
 * @param testRootPath
 */
@Serializable
data class TestSuitesSourceDto(
    val organizationName: String,
    val name: String,
    val description: String?,
    val gitDto: GitDto,
    val branch: String,
    val testRootPath: String,
)
