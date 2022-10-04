/**
 * This file contains actual classes from jvmMain
 */

package com.saveourtool.save.utils

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.entities.AgentStatusDto
import javax.persistence.EnumType

actual typealias EnumType = EnumType

actual typealias LocalDateTime = java.time.LocalDateTime

/**
 * @param containerId
 * @return [AgentStatusDto] with state from receiver
 */
fun AgentState.newFor(containerId: String): AgentStatusDto = AgentStatusDto(
    time = LocalDateTime.now(),
    state = this,
    containerId = containerId,
)
