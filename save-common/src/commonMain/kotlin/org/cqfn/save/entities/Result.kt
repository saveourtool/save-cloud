package org.cqfn.save.entities

import org.cqfn.save.domain.ResultStatus

import kotlinx.serialization.Serializable

/**
 * @property id id of test
 * @property status of test
 * @property date date of result
 */
@Entity
@Serializable
data class Result(
    @Id val id: Int,
    val status: ResultStatus,
    val date: String  // fixme should be date
)
