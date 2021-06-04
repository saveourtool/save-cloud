package org.cqfn.save.execution

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @property version
 * @property status
 * @property type
 */
@Serializable
class ExecutionDto(
    val status: ExecutionStatus,
    val type: ExecutionType,
    val version: String,
) {
    override fun toString() = Json.encodeToString(this)
}
