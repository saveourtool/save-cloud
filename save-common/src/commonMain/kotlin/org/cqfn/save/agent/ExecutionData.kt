package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * @property jobIds
 */
@Serializable data class ExecutionData(val jobIds: List<Int>)
