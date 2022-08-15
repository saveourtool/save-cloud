package com.saveourtool.save.testsuite

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.validation.Validatable
import com.saveourtool.save.validation.isValidName
import com.saveourtool.save.validation.isValidPath
import kotlinx.serialization.Serializable

typealias TestSuitesSourceDtoList = List<TestSuitesSourceDto>

/**
 * @property organizationName
 * @property name
 * @property description
 * @property gitDto
 * @property branch
 * @property testRootPath
 */
@Serializable
data class TestSuitesSourceDto(
    val organizationName: String,
    val name: String,
    val description: String?,
    val gitDto: GitDto,
    val branch: String,
    val testRootPath: String,
): Validatable {
    override fun validate(): Boolean = validateName() && validateTestRootPath()

    fun validateName(): Boolean = name.isValidName()

    fun validateTestRootPath(): Boolean = testRootPath.isValidPath()

    companion object {
        val empty = TestSuitesSourceDto(
            "",
            "",
            "",
            GitDto.empty,
            "",
            "",
        )
    }
}
