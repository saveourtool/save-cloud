package com.saveourtool.save.orchestrator.sandbox

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatus
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.orchestrator.service.AgentStatusList
import com.saveourtool.save.orchestrator.service.BridgeService
import com.saveourtool.save.orchestrator.service.IdList
import com.saveourtool.save.orchestrator.service.TestExecutionList
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto
import reactor.core.publisher.Mono

class SandboxBridgeService : BridgeService {
    override fun getNextTestBatch(agentId: String): Mono<TestBatch> {
        TODO("Not yet implemented")
    }

    override fun addAgents(agents: List<Agent>): Mono<IdList> {
        TODO("Not yet implemented")
    }

    override fun updateAgentStatuses(agentStates: List<AgentStatus>): Mono<Unit> {
        TODO("Not yet implemented")
    }

    override fun updateAgentStatusesWithDto(agentState: AgentStatusDto): Mono<BodilessResponseEntity> {
        TODO("Not yet implemented")
    }

    override fun getReadyForTestingTestExecutions(agentId: String): Mono<TestExecutionList> {
        TODO("Not yet implemented")
    }

    override fun getAgentsStatuses(executionId: Long, agentIds: List<String>): Mono<AgentStatusList> {
        TODO("Not yet implemented")
    }

    override fun updateExecutionByDto(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?
    ): Mono<BodilessResponseEntity> {
        TODO("Not yet implemented")
    }

    override fun getAgentsStatusesForSameExecution(agentId: String): Mono<AgentStatusesForExecution> {
        TODO("Not yet implemented")
    }

    override fun assignAgent(agentId: String, testDtos: List<TestDto>): Mono<BodilessResponseEntity> {
        TODO("Not yet implemented")
    }

    override fun setStatusByAgentIds(agentsList: Collection<String>, status: AgentState): Mono<BodilessResponseEntity> {
        TODO("Not yet implemented")
    }
}