package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentInitConfig
import com.saveourtool.save.agent.AgentRunConfig
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.AgentDto
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.spring.utils.applyAll
import com.saveourtool.save.utils.*
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * Service for work with agents and backend
 */
@Component
class BackendOrchestratorAgentService(
    @Value("\${orchestrator.backend-url}") private val backendUrl: String,
    customizers: List<WebClientCustomizer>,
) : OrchestratorAgentService {
    private val webClientBackend = WebClient.builder()
        .baseUrl(backendUrl)
        .applyAll(customizers)
        .build()

    override fun getInitConfig(containerId: String): Mono<AgentInitConfig> = webClientBackend
        .get()
        .uri("/agents/get-init-config?containerId=$containerId")
        .retrieve()
        .bodyToMono()

    override fun getNextRunConfig(containerId: String): Mono<AgentRunConfig> = webClientBackend
        .get()
        .uri("/agents/get-next-run-config?containerId=$containerId")
        .retrieve()
        .bodyToMono()

    override fun addAgents(executionId: Long, agents: List<AgentDto>): Mono<IdList> = webClientBackend
        .post()
        .uri("/agents/insert?executionId=$executionId")
        .bodyValue(agents)
        .retrieve()
        .bodyToMono()

    override fun updateAgentStatusesWithDto(agentStates: List<AgentStatusDto>): Mono<EmptyResponse> =
            webClientBackend
                .post()
                .uri("/updateAgentStatusesWithDto")
                .bodyValue(agentStates)
                .retrieve()
                .toBodilessEntity()

    override fun getReadyForTestingTestExecutions(containerId: String): Mono<TestExecutionList> = webClientBackend.get()
        .uri("/test-executions/get-by-container-id?containerId=$containerId&status=${TestResultStatus.READY_FOR_TESTING}")
        .retrieve()
        .bodyToMono()

    override fun getAgentsStatuses(
        containerIds: List<String>,
    ): Mono<AgentStatusList> = webClientBackend
        .get()
        .uri("/agents/statuses?ids=${containerIds.joinToString(separator = DATABASE_DELIMITER)}")
        .retrieve()
        .bodyToMono()

    override fun updateExecutionByDto(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?,
    ): Mono<EmptyResponse> =
            webClientBackend.post()
                .uri("/updateExecutionByDto")
                .bodyValue(ExecutionUpdateDto(executionId, executionStatus, failReason))
                .retrieve()
                .toBodilessEntity()

    override fun getAgentStatusesByExecutionId(executionId: Long): Mono<AgentStatusesForExecution> = webClientBackend
        .get()
        .uri("/getAgentStatusesByExecutionId?executionId=$executionId")
        .retrieve()
        .bodyToMono()

    override fun getAgentStatusesForSameExecution(containerId: String): Mono<AgentStatusesForExecution> = webClientBackend
        .get()
        .uri("/getAgentsStatusesForSameExecution?containerId=$containerId")
        .retrieve()
        .bodyToMono()

    override fun markTestExecutionsOfAgentsAsFailed(containerIds: Collection<String>, onlyReadyForTesting: Boolean): Mono<EmptyResponse> {
        log.debug("Attempt to mark test executions of agents=$containerIds as failed with internal error")
        return webClientBackend.post()
            .uri("/test-executions/mark-as-failed-by-container-ids?onlyReadyForTesting=$onlyReadyForTesting")
            .bodyValue(containerIds)
            .retrieve()
            .toBodilessEntity()
    }

    companion object {
        private val log: Logger = getLogger<BackendOrchestratorAgentService>()
    }
}
