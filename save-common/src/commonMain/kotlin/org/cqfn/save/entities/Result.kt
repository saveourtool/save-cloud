package org.cqfn.save.entities

import kotlinx.serialization.Serializable
import org.cqfn.save.domain.ResultStatus

@Entity
@Serializable
data class Result(
    @Id val id: Int,
    val status: ResultStatus,
    val date: String // fixme should be date
)
