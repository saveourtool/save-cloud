package org.cqfn.save.orchestrator.model

import kotlinx.serialization.Serializable

@Serializable
enum class AgentState {
    BUSY,
    ERROR,
    FINISHED,
    IDLE,
    ;
}
