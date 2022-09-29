package com.saveourtool.save.sandbox.service

import com.saveourtool.save.agent.AgentInitConfig
import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.entities.AgentDto
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.entities.TestExecution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.service.AgentStatusList
import com.saveourtool.save.orchestrator.service.IdList
import com.saveourtool.save.orchestrator.service.TestExecutionList
import com.saveourtool.save.sandbox.entity.toEntity
import com.saveourtool.save.sandbox.repository.SandboxAgentRepository
import com.saveourtool.save.sandbox.repository.SandboxAgentStatusRepository
import com.saveourtool.save.sandbox.repository.SandboxExecutionRepository
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.orConflict
import com.saveourtool.save.utils.orNotFound
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

internal typealias BodilessResponseEntity = ResponseEntity<Void>

/**
 * Sandbox implementation for agent service
 */
@Component
class SandboxAgentRepository(
    private val sandboxAgentRepository: SandboxAgentRepository,
    private val sandboxAgentStatusRepository: SandboxAgentStatusRepository,
    private val sandboxExecutionRepository: SandboxExecutionRepository,
): com.saveourtool.save.orchestrator.service.AgentRepository {
    override fun getInitConfig(containerId: String): Mono<AgentInitConfig> = Mono.empty()

    override fun getNextTestBatch(containerId: String): Mono<TestBatch> = Mono.empty()

    override fun addAgents(agents: List<AgentDto>): Mono<IdList> = blockingToMono {
        agents
            .map { it.toEntity(this::getExecution) }
            .let { sandboxAgentRepository.saveAll(it) }
            .map { it.requiredId() }
    }

    override fun updateAgentStatusesWithDto(agentStates: List<AgentStatusDto>): Mono<BodilessResponseEntity> = blockingToMono {
        agentStates
            .map { it.toEntity(this::getAgent) }
            .let { sandboxAgentStatusRepository.saveAll(it) }
            .let {
                ResponseEntity.ok().build()
            }
    }

    override fun getReadyForTestingTestExecutions(containerId: String): Mono<TestExecutionList> = Mono.fromCallable {
        // sandbox doesn't have TestExecution at all
        emptyList()
    }

    override fun getAgentsStatuses(containerIds: List<String>): Mono<AgentStatusList> = blockingToMono {
        containerIds
            .mapNotNull { sandboxAgentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(it) }
            .map { it.toDto() }
    }

    override fun updateExecutionByDto(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?
    ): Mono<BodilessResponseEntity> = Mono.empty()

    override fun getAgentsStatusesForSameExecution(containerId: String): Mono<AgentStatusesForExecution> = blockingToMono {
        val execution = getAgent(containerId).execution
        sandboxAgentRepository.findByExecutionId(execution.requiredId())
            .map { agent ->
                sandboxAgentStatusRepository.findTopByAgentContainerIdOrderByEndTimeDescIdDesc(agent.containerId)
                    .orNotFound {
                        "AgentStatus not found for agent id=${agent.containerId}"
                    }
            }
            .map {
                it.toDto()
            }
            .let {
                AgentStatusesForExecution(execution.requiredId(), it)
            }
    }

    override fun setStatusByAgentIds(containerIds: Collection<String>, status: AgentState): Mono<BodilessResponseEntity> = Mono.fromCallable {
        // sandbox doesn't have TestExecution
        ResponseEntity.ok().build()
    }

    /**
     * @param executionId
     * @return userName for provided [executionId]
     */
    fun getUserNameByExecutionId(executionId: Long): String = getExecution(executionId)
        .user
        .name
        .orConflict {
            "All users should have name"
        }

    private fun getExecution(executionId: Long) = sandboxExecutionRepository
        .findByIdOrNull(executionId)
        .orNotFound { "No execution with id $executionId" }

    private fun getAgent(containerId: String) = sandboxAgentRepository
        .findByContainerId(containerId)
        .orNotFound { "No agent with containerId $containerId" }
}
