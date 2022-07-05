package com.saveourtool.save.entities

import com.saveourtool.save.utils.EnumType
import com.saveourtool.save.utils.LocalDateTime

import kotlinx.serialization.Contextual

/**
 * @property name organization
 * @property status
 * @property startTime the time contest starts
 * @property endTime the time contest ends
 * @property description
 * @property testSuiteIds
 */
@Entity
@Suppress("USE_DATA_CLASS")
data class Contest(
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: ContestStatus,
    @Contextual
    var startTime: LocalDateTime?,
    @Contextual
    var endTime: LocalDateTime?,
    var description: String? = null,
    var testSuiteIds: String? = null,
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
    )

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
        ).apply {
            this.id = id
        }
    }
}
