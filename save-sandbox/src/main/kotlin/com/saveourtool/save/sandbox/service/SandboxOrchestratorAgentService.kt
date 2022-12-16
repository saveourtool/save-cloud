package com.saveourtool.save.sandbox.service

import com.saveourtool.save.agent.AgentInitConfig
import com.saveourtool.save.agent.AgentRunConfig
import com.saveourtool.save.agent.SaveCliOverrides
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentDto
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.entities.toEntity
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.service.AgentStatusList
import com.saveourtool.save.orchestrator.service.IdList
import com.saveourtool.save.orchestrator.service.OrchestratorAgentService
import com.saveourtool.save.orchestrator.service.TestExecutionList
import com.saveourtool.save.request.RunExecutionRequest
import com.saveourtool.save.sandbox.config.ConfigProperties
import com.saveourtool.save.sandbox.entity.SandboxExecution
import com.saveourtool.save.sandbox.entity.SandboxLnkExecutionAgent
import com.saveourtool.save.sandbox.repository.SandboxAgentRepository
import com.saveourtool.save.sandbox.repository.SandboxAgentStatusRepository
import com.saveourtool.save.sandbox.repository.SandboxExecutionRepository
import com.saveourtool.save.sandbox.repository.SandboxLnkExecutionAgentRepository
import com.saveourtool.save.sandbox.storage.SandboxStorage
import com.saveourtool.save.sandbox.storage.SandboxStorageKeyType
import com.saveourtool.save.utils.*

import generated.SAVE_CORE_VERSION
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.util.stream.Collectors

/**
 * Sandbox implementation for agent service
 */
@Component("SandboxAgentRepositoryForOrchestrator")
class SandboxOrchestratorAgentService(
    private val sandboxAgentRepository: SandboxAgentRepository,
    private val sandboxAgentStatusRepository: SandboxAgentStatusRepository,
    private val sandboxLnkExecutionAgentRepository: SandboxLnkExecutionAgentRepository,
    private val sandboxExecutionRepository: SandboxExecutionRepository,
    private val sandboxStorage: SandboxStorage,
    configProperties: ConfigProperties,
) : OrchestratorAgentService {
    private val sandboxUrlForAgent = "${configProperties.agentSettings.sandboxUrl}/sandbox/internal"

    override fun getInitConfig(containerId: String): Mono<AgentInitConfig> = getExecutionAsMonoByContainerId(containerId)
        .zipWhen { execution ->
            sandboxStorage.list(execution.userId, SandboxStorageKeyType.FILE)
                .map { storageKey ->
                    storageKey.fileName to "$sandboxUrlForAgent/download-file?userId=${storageKey.userId}&fileName=${storageKey.fileName}"
                }
                .collectList()
                .map {
                    it.toMap()
                }
        }
        .map { (execution, fileToUrls) ->
            AgentInitConfig(
                saveCliUrl = "$sandboxUrlForAgent/download-save-cli?version=$SAVE_CORE_VERSION",
                testSuitesSourceSnapshotUrl = "$sandboxUrlForAgent/download-test-files?userId=${execution.userId}",
                additionalFileNameToUrl = fileToUrls,
                // sandbox doesn't support save-cli overrides for now
                saveCliOverrides = SaveCliOverrides(),
            )
        }

    override fun getNextRunConfig(containerId: String): Mono<AgentRunConfig> = getExecutionAsMonoByContainerId(containerId)
        .filter { !it.initialized }
        .map { execution -> sandboxExecutionRepository.save(execution.apply { initialized = true }) }
        .flatMap { execution ->
            sandboxStorage.list(execution.userId, SandboxStorageKeyType.TEST)
                .map { it.fileName }
                .filter { it.endsWith("save.toml") }
                .collect(Collectors.joining(" "))
                .zipWith(execution.userId.toMono())
        }
        .map { (cliArgs, userId) ->
            AgentRunConfig(
                cliArgs = cliArgs,
                executionDataUploadUrl = "$sandboxUrlForAgent/upload-execution-data",
                debugInfoUploadUrl = "$sandboxUrlForAgent/upload-debug-info?userId=$userId",
            )
        }

    override fun addAgents(executionId: Long, agents: List<AgentDto>): Mono<IdList> = blockingToMono {
        val execution = sandboxExecutionRepository.findByIdOrNull(executionId)
            .orNotFound {
                "Not found execution by id $executionId"
            }
        agents
            .map { it.toEntity() }
            .let { sandboxAgentRepository.saveAll(it) }
            .map {
                SandboxLnkExecutionAgent(
                    execution = execution,
                    agent = it
                )
            }
            .let { sandboxLnkExecutionAgentRepository.saveAll(it) }
            .map { it.agent.requiredId() }
    }

    override fun updateAgentStatusesWithDto(agentStates: List<AgentStatusDto>): Mono<EmptyResponse> = blockingToMono {
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
    ): Mono<EmptyResponse> = getExecutionAsMono(executionId)
        .map { execution ->
            sandboxExecutionRepository.save(
                execution.apply {
                    this.status = executionStatus
                    this.failReason = failReason
                }
            )
        }
        .thenReturn(ResponseEntity.ok().build())

    override fun getAgentStatusesByExecutionId(executionId: Long): Mono<AgentStatusesForExecution> = getExecutionAsMono(executionId)
        .mapToAgentStatuses()

    override fun getAgentStatusesForSameExecution(containerId: String): Mono<AgentStatusesForExecution> = getExecutionAsMonoByContainerId(containerId)
        .mapToAgentStatuses()

    fun Mono<SandboxExecution>.mapToAgentStatuses(): Mono<AgentStatusesForExecution> = this
        .map { execution ->
            sandboxLnkExecutionAgentRepository.findByExecutionId(execution.requiredId())
                .map { it.agent }
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

    override fun markTestExecutionsOfAgentsAsFailed(containerIds: Collection<String>, onlyReadyForTesting: Boolean): Mono<EmptyResponse> = Mono.fromCallable {
        // sandbox doesn't have TestExecution
        ResponseEntity.ok().build()
    }

    /**
     * @param execution
     * @return a request to run execution
     */
    fun getRunRequest(execution: SandboxExecution): RunExecutionRequest = execution.toRunRequest(
        saveAgentVersion = SAVE_CORE_VERSION,
        saveAgentUrl = "$sandboxUrlForAgent/download-save-agent",
    )

    private fun getExecution(executionId: Long): SandboxExecution = sandboxExecutionRepository
        .findByIdOrNull(executionId)
        .orNotFound { "No execution with id $executionId" }

    private fun getExecutionAsMono(executionId: Long): Mono<SandboxExecution> = blockingToMono {
        getExecution(executionId)
    }

    private fun getAgent(containerId: String): Agent = sandboxAgentRepository
        .findByContainerId(containerId)
        .orNotFound {
            "No agent with containerId $containerId"
        }

    private fun getExecutionAsMonoByContainerId(containerId: String): Mono<SandboxExecution> = blockingToMono {
        sandboxLnkExecutionAgentRepository.findByAgentId(getAgent(containerId).requiredId())
            .orNotFound {
                "No linked execution to agent with containerId $containerId"
            }
            .execution
    }
}
