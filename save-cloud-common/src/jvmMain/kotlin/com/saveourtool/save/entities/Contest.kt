package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.utils.LocalDateTime
import com.saveourtool.save.validation.isValidName
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property name organization
 * @property status
 * @property startTime the time contest starts
 * @property endTime the time contest ends
 * @property organization organization that created this contest
 * @property description
 * @property creationTime
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
    var description: String? = null,
    var creationTime: LocalDateTime?,
) : BaseEntity() {
    /**
     * Create Data Transfer Object in order to pass entity to frontend
     *
     * @param testSuiteIds list of test suite ids that are connected with this Contest
     * @return [ContestDto]
     */
    @Suppress("UnsafeCallOnNullableType")
    fun toDto(testSuiteIds: List<Long>) = ContestDto(
        name,
        status,
        startTime!!,
        endTime!!,
        description,
        organization.name,
        testSuiteIds,
        creationTime,
    )

    private fun validateDateRange() = startTime != null && endTime != null && (startTime as LocalDateTime) < endTime

    /**
     * Validate contest data
     *
     * @return true if contest data is valid, false otherwise
     */
    override fun validate() = name.isValidName() && validateDateRange()

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
            organization = Organization.stub(1),
            creationTime = LocalDateTime.now(),
        ).apply {
            this.id = id
        }

        /**
         * Create [Contest] from [ContestDto]
         *
         * @param organization that created contest
         * @param creationTime specified time when contest was created
         * @return [Contest] entity
         */
        fun ContestDto.toContest(
            organization: Organization,
            creationTime: LocalDateTime? = null,
        ) = Contest(
            name,
            status,
            startTime,
            endTime,
            organization,
            description,
            creationTime ?: this.creationTime,
        )
    }
}
