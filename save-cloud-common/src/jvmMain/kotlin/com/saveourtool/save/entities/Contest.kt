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
) {
    /**
     * id of contest
     */
    @Id
    @GeneratedValue
    var id: Long? = null

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
