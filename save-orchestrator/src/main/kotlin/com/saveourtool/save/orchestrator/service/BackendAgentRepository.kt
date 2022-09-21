package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentInitConfig
import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.entities.AgentStatusesForExecution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.utils.*

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * Service for work with agents and backend
 */
@Component
class BackendAgentRepository(
    configProperties: ConfigProperties,
) : AgentRepository {
    private val webClientBackend = WebClient.create(configProperties.backendUrl)
    override fun getInitConfig(agentId: String): Mono<AgentInitConfig> = webClientBackend
        .get()
        .uri("/agents/get-init-config?agentId=$agentId")
        .retrieve()
        .bodyToMono()

    override fun getNextTestBatch(agentId: String): Mono<TestBatch> = webClientBackend
        .get()
        .uri("/getTestBatches?agentId=$agentId")
        .retrieve()
        .bodyToMono()

    override fun addAgents(agents: List<Agent>): Mono<IdList> = webClientBackend
        .post()
        .uri("/addAgents")
        .body(BodyInserters.fromValue(agents))
        .retrieve()
        .bodyToMono()

    override fun updateAgentStatusesWithDto(agentStates: List<AgentStatusDto>): Mono<BodilessResponseEntity> =
            webClientBackend
                .post()
                .uri("/updateAgentStatusesWithDto")
                .bodyValue(agentStates)
                .retrieve()
                .toBodilessEntity()

    override fun getReadyForTestingTestExecutions(agentId: String): Mono<TestExecutionList> = webClientBackend.get()
        .uri("/testExecutions/agent/$agentId/${TestResultStatus.READY_FOR_TESTING}")
        .retrieve()
        .bodyToMono()

    override fun getAgentsStatuses(
        executionId: Long,
        agentIds: List<String>,
    ): Mono<AgentStatusList> = webClientBackend
        .get()
        .uri("/agents/statuses?ids=${agentIds.joinToString(separator = DATABASE_DELIMITER)}")
        .retrieve()
        .bodyToMono()

    override fun updateExecutionByDto(
        executionId: Long,
        executionStatus: ExecutionStatus,
        failReason: String?,
    ): Mono<BodilessResponseEntity> =
            webClientBackend.post()
                .uri("/updateExecutionByDto")
                .bodyValue(ExecutionUpdateDto(executionId, executionStatus, failReason))
                .retrieve()
                .toBodilessEntity()

    override fun getAgentsStatusesForSameExecution(agentId: String): Mono<AgentStatusesForExecution> = webClientBackend
        .get()
        .uri("/getAgentsStatusesForSameExecution?agentId=$agentId")
        .retrieve()
        .bodyToMono()

    override fun assignAgent(agentId: String, testDtos: List<TestDto>): Mono<BodilessResponseEntity> = webClientBackend.post()
        .uri("/testExecution/assignAgent?agentContainerId=$agentId")
        .bodyValue(testDtos)
        .retrieve()
        .toBodilessEntity()

    override fun setStatusByAgentIds(agentIds: Collection<String>, status: AgentState): Mono<BodilessResponseEntity> =
            webClientBackend.post()
                .uri("/testExecution/setStatusByAgentIds?status=${status.name}")
                .bodyValue(agentIds)
                .retrieve()
                .toBodilessEntity()
}
