package org.cqfn.save.agent

import kotlinx.serialization.Serializable

/**
 * @property cliLogs
 */
@Serializable
data class ExecutionLogs(val cliLogs: List<String>)
