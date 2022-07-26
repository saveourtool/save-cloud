package com.saveourtool.save.testsuite

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Organization
import kotlinx.serialization.Serializable

/**
 * @param organization
 * @param name
 * @param description
 * @param gitDto
 * @param branch
 * @param testRootPath
 */
@Serializable
data class TestSuitesSourceDto(
    val organization: Organization,
    val name: String,
    val description: String?,
    val gitDto: GitDto,
    val branch: String,
    val testRootPath: String,
) {
    companion object {
        /**
         * @return default name fot [com.saveourtool.save.entities.TestSuitesSource]
         */
        fun defaultTestSuitesSourceName(
            url: String,
            branch: String,
            subDirectory: String
        ): String = buildString {
            append(url)
            append("/tree/")
            append(branch)
            if (subDirectory.isNotBlank()) {
                append("/$subDirectory")
            }
        }
    }
}
