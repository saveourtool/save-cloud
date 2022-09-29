package com.saveourtool.save.sandbox.service

import com.saveourtool.save.agent.AgentInitConfig
import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.entities.AgentDto
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.service.AgentRepository
import com.saveourtool.save.orchestrator.service.AgentStatusList
import com.saveourtool.save.orchestrator.service.IdList
import com.saveourtool.save.orchestrator.service.TestExecutionList
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto
import org.springframework.http.ResponseEntity

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

internal typealias BodilessResponseEntity = ResponseEntity<Void>

/**
 * Sandbox implementation for agent service
 */
@Component
class SandboxAgentRepository : AgentRepository {
    override fun getInitConfig(containerId: String): Mono<AgentInitConfig> = Mono.empty()

    override fun getNextRunConfig(containerId: String): Mono<TestBatch> = Mono.empty()

    override fun addAgents(agents: List<AgentDto>): Mono<IdList> = Mono.empty()

    override fun updateAgentStatusesWithDto(agentStates: List<AgentStatusDto>): Mono<BodilessResponseEntity> = Mono.empty()

    override fun getReadyForTestingTestExecutions(agentId: String): Mono<TestExecutionList> = Mono.empty()

    override fun getAgentsStatuses(executionId: Long, agentIds: List<String>): Mono<AgentStatusList> = Mono.empty()

    override fun updateExecutionByDto(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?
    ): Mono<BodilessResponseEntity> = Mono.empty()

    override fun getAgentsStatusesForSameExecution(agentId: String): Mono<AgentStatusesForExecution> = Mono.empty()

    override fun assignAgent(agentId: String, testDtos: List<TestDto>): Mono<BodilessResponseEntity> = Mono.empty()

    override fun setStatusByAgentIds(agentIds: Collection<String>, status: AgentState): Mono<BodilessResponseEntity> = Mono.empty()

    /**
     * @param executionId
     * @return userName for provided [executionId]
     */
    fun getUserNameByExecutionId(executionId: Long): String = TODO("Need to add database for mapping")
}
