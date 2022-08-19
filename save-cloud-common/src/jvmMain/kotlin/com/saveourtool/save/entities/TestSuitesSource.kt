package com.saveourtool.save.entities

import com.saveourtool.save.testsuite.TestSuitesSourceDto
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @param organization which this test suites source belongs to
 * @param name unique name of [TestSuitesSource]
 * @param description free text
 * @param git git credentials for this test suites source
 * @param branch branch which is used for this test suites source
 * @param testRootPath relative path to tests in source
 * @property organization
 * @property name
 * @property description
 * @property git
 * @property branch
 * @property testRootPath
 */
@Entity
class TestSuitesSource(
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    var organization: Organization,

    var name: String,
    var description: String?,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "git_id")
    var git: Git,
    var branch: String,
    var testRootPath: String,
) : BaseEntity() {
    /**
     * @return entity as dto [TestSuitesSourceDto]
     */
    fun toDto(): TestSuitesSourceDto = TestSuitesSourceDto(
        organizationName = organization.name,
        name = name,
        description = description,
        gitDto = git.toDto(),
        branch = branch,
        testRootPath = testRootPath,
    )

    companion object {
        val empty = TestSuitesSource(
            Organization.stub(-1),
            "",
            null,
            Git.empty,
            "",
            "",
        )

        /**
         * @param organization [Organization] from database
         * @param git [Git] from database
         * @return [TestSuitesSource] from [TestSuitesSourceDto]
         */
        fun TestSuitesSourceDto.toTestSuiteSource(
            organization: Organization,
            git: Git,
        ) = TestSuitesSource(
            organization,
            name,
            description,
            git,
            branch,
            testRootPath,
        )
    }
}
