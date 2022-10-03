package com.saveourtool.save.sandbox.service

import com.saveourtool.save.agent.AgentInitConfig
import com.saveourtool.save.agent.SaveCliOverrides
import com.saveourtool.save.entities.AgentDto
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.service.AgentStatusList
import com.saveourtool.save.orchestrator.service.IdList
import com.saveourtool.save.orchestrator.service.TestExecutionList
import com.saveourtool.save.sandbox.entity.SandboxExecution
import com.saveourtool.save.sandbox.entity.toEntity
import com.saveourtool.save.sandbox.repository.SandboxAgentRepository
import com.saveourtool.save.sandbox.repository.SandboxAgentStatusRepository
import com.saveourtool.save.sandbox.repository.SandboxExecutionRepository
import com.saveourtool.save.sandbox.repository.SandboxUserRepository
import com.saveourtool.save.sandbox.storage.SandboxStorage
import com.saveourtool.save.sandbox.storage.SandboxStorageKeyType
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.utils.*

import generated.SAVE_CORE_VERSION
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

internal typealias BodilessResponseEntity = ResponseEntity<Void>

/**
 * Sandbox implementation for agent service
 */
@Component("SandboxAgentRepositoryForOrchestrator")
class SandboxAgentRepository(
    private val sandboxAgentRepository: SandboxAgentRepository,
    private val sandboxAgentStatusRepository: SandboxAgentStatusRepository,
    private val sandboxExecutionRepository: SandboxExecutionRepository,
    private val sandboxUserRepository: SandboxUserRepository,
    private val sandboxStorage: SandboxStorage,
    @Value("sandbox.url") private val sandboxUrl: String,
) : com.saveourtool.save.orchestrator.service.AgentRepository {
    override fun getInitConfig(containerId: String): Mono<AgentInitConfig> = blockingToMono {
        getAgent(containerId).execution
    }
        .zipWhen { execution ->
            sandboxStorage.list(execution.getUserName(), SandboxStorageKeyType.FILE)
                .map { storageKey ->
                    storageKey.fileName to "$sandboxUrl/sandbox/internal/download-file?userName=${storageKey.userName}&fileName=${storageKey.fileName}"
                }
                .collectList()
                .map {
                    it.toMap()
                }
        }
        .map { (execution, fileToUrls) ->
            val userName = execution.getUserName()
            AgentInitConfig(
                saveCliUrl = "$sandboxUrl/sandbox/internal/download-save-cli?version=$SAVE_CORE_VERSION",
                testSuitesSourceSnapshotUrl = "$sandboxUrl/sandbox/internal/download-test-files?userName=$userName",
                additionalFileNameToUrl = fileToUrls,
                // sandbox doesn't support save-cli overrides for now
                saveCliOverrides = SaveCliOverrides(),
            )
        }

    override fun getNextTestBatch(containerId: String): Mono<TestBatch> = blockingToMono {
        getAgent(containerId).execution
    }
        .flatMap { execution ->
            sandboxStorage.list(execution.getUserName(), SandboxStorageKeyType.TEST)
                .map { it.fileName }
                .map { fileName ->
                    TestDto(
                        filePath = fileName,
                        pluginName = com.saveourtool.save.plugin.warn.WarnPlugin::class.simpleName ?: "N/A",
                        testSuiteId = -1,
                        hash = "N/A",
                        additionalFiles = emptyList(),
                    )
                }
                .collectList()
        }

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
    ): Mono<BodilessResponseEntity> = blockingToMono {
        getExecution(executionId)
            .let { execution ->
                sandboxExecutionRepository.save(
                    execution.apply {
                        this.status = executionStatus
                        this.failReason = failReason
                    }
                )
            }
            .let {
                ResponseEntity.ok().build()
            }
    }

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

    override fun markTestExecutionsOfAgentsAsFailed(containerIds: Collection<String>, onlyReadyForTesting: Boolean): Mono<BodilessResponseEntity> = Mono.fromCallable {
        // sandbox doesn't have TestExecution
        ResponseEntity.ok().build()
    }

    /**
     * @param executionId
     * @return userName for provided [executionId]
     */
    fun getUserNameByExecutionId(executionId: Long): String = getExecution(executionId).getUserName()

    private fun SandboxExecution.getUserName(): String = sandboxUserRepository.getNameById(userId)

    private fun getExecution(executionId: Long) = sandboxExecutionRepository
        .findByIdOrNull(executionId)
        .orNotFound { "No execution with id $executionId" }

    private fun getAgent(containerId: String) = sandboxAgentRepository
        .findByContainerId(containerId)
        .orNotFound { "No agent with containerId $containerId" }
}
