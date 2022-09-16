package com.saveourtool.save.orchestrator.sandbox

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.HeartbeatResponse
import com.saveourtool.save.agent.NewJobResponse
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.orchestrator.ExecutionIdToAgentIds
import com.saveourtool.save.orchestrator.service.AgentService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

/**
 * Service for work with agents in Sandbox mode
 */
@Service
@Profile(SANDBOX_PROFILE)
class SandboxAgentService : AgentService {
    private val agents: Map<String, String> = mutableMapOf()

    /**
     * A scheduler that executes long-running background tasks
     */
    override val scheduler = Schedulers.boundedElastic().also { it.start() }

    override fun getNewTestsIds(agentId: String): Mono<HeartbeatResponse> {
        TODO("Not yet implemented")
    }

    override fun saveAgentsWithInitialStatuses(agents: List<Agent>): Mono<Void> {
        TODO("Not yet implemented")
    }

    override fun updateAgentStatuses(agentStates: List<AgentStatus>): Mono<Void> {
        TODO("Not yet implemented")
    }

    override fun updateAgentStatusesWithDto(agentState: AgentStatusDto): Mono<BodilessResponseEntity> {
        TODO("Not yet implemented")
    }

    override fun checkSavedData(agentId: String): Mono<Boolean> {
        TODO("Not yet implemented")
    }

    override fun finalizeExecution(agentId: String) {
        TODO("Not yet implemented")
    }

    override fun markExecutionBasedOnAgentStates(
        executionId: Long,
        agentIds: List<String>
    ): Mono<BodilessResponseEntity> {
        TODO("Not yet implemented")
    }

    override fun updateExecution(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?
    ): Mono<BodilessResponseEntity> {
        TODO("Not yet implemented")
    }

    override fun getFinishedOrStoppedAgentsForSameExecution(agentId: String): Mono<ExecutionIdToAgentIds> {
        TODO("Not yet implemented")
    }

    override fun areAllAgentsIdleOrFinished(agentId: String): Mono<Boolean> {
        TODO("Not yet implemented")
    }

    override fun updateAssignedAgent(agentContainerId: String, newJobResponse: NewJobResponse) {
        TODO("Not yet implemented")
    }

    override fun markTestExecutionsAsFailed(
        agentsList: Collection<String>,
        status: AgentState
    ): Mono<BodilessResponseEntity> {
        TODO("Not yet implemented")
    }

}