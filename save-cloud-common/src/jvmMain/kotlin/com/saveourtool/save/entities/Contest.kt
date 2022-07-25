package com.saveourtool.save.entities

import com.saveourtool.save.utils.EnumType
import com.saveourtool.save.utils.LocalDateTime

/**
 * @property name organization
 * @property status
 * @property startTime the time contest starts
 * @property endTime the time contest ends
 * @property organization organization that created this contest
 * @property description
 * @property testSuiteIds
 */
@Entity
@Suppress("LongParameterList")
class Contest(
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: ContestStatus,
    var startTime: LocalDateTime?,
    var endTime: LocalDateTime?,
    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,
    var testSuiteIds: String = "",
    var description: String? = null,
) {
    /**
     * id of contest
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    /**
     * Create Data Transfer Object in order to pass entity to frontend
     *
     * @return [ContestDto]
     */
    @Suppress("UnsafeCallOnNullableType")
    fun toDto() = ContestDto(
        name,
        startTime!!,
        endTime!!,
        description,
        organization.name,
    )

    /**
     * @return set of testSuiteIds
     */
    fun getTestSuiteIds() = testSuiteIds.split(",")
        .mapNotNull {
            it.toLongOrNull()
        }
        .toSet()

    companion object {
        /**
         * Create a stub for testing.
         *
         * @param id id of created
         * @param status
         * @return an organization
         */
        fun stub(
            id: Long?,
            status: ContestStatus = ContestStatus.CREATED
        ) = Contest(
            name = "stub",
            status = status,
            startTime = null,
            endTime = null,
            organization = Organization.stub(1)
        ).apply {
            this.id = id
        }

        /**
         * Create [Contest] from [ContestDto]
         *
         * @param organization that created contest
         * @param testSuiteIds list of test suite ids
         * @param status [ContestStatus]
         * @return [Contest] entity
         */
        fun ContestDto.toContest(
            organization: Organization,
            testSuiteIds: List<Long> = emptyList(),
            status: ContestStatus = ContestStatus.CREATED,
        ) = Contest(
            name,
            status,
            startTime,
            endTime,
            organization,
            testSuiteIds.joinToString(","),
            description,
        )
    }
}
