package org.cqfn.save.agent

import kotlinx.serialization.Serializable

@Serializable data class ExecutionData(val jobIds: List<Int>)
