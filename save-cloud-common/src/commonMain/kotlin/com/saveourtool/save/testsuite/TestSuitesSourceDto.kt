package com.saveourtool.save.testsuite

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Organization
import kotlinx.serialization.Serializable

@Serializable
data class TestSuitesSourceDto(
    val organization: Organization,
    val name: String,
    val description: String?,
    val gitDto: GitDto,
    val branch: String,
    val testRootPath: String,
) {
    constructor(
        organization: Organization,
        description: String?,
        gitDto: GitDto,
        branch: String,
        testRootPath: String,
    ) : this(
        organization = organization,
        name = defaultTestSuitesSourceName(gitDto.url, branch, testRootPath),
        description = description,
        gitDto = gitDto,
        branch = branch,
        testRootPath = testRootPath,
    )

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
