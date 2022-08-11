package com.saveourtool.save.backend.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.saveourtool.save.backend.IdResponse
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.ExecutionInfoStorage
import com.saveourtool.save.backend.utils.blockingToMono
import com.saveourtool.save.backend.utils.justOrNotFound
import com.saveourtool.save.backend.utils.username
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.ExecutionRunRequest
import com.saveourtool.save.entities.ExecutionRunRequestByContest
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.DATABASE_DELIMITER
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.switchIfEmptyToResponseException
import com.saveourtool.save.v1
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@RestController
@RequestMapping("/api/$v1/run")
class RunExecutionController(
    private val projectService: ProjectService,
    private val executionService: ExecutionService,
    private val executionInfoStorage: ExecutionInfoStorage,
    private val testService: TestService,
    private val testExecutionService: TestExecutionService,
    private val meterRegistry: MeterRegistry,
    configProperties: ConfigProperties,
    objectMapper: ObjectMapper,
) {
    private val webClientOrchestrator = WebClient.builder()
        .baseUrl(configProperties.orchestratorUrl)
        .codecs {
            it.defaultCodecs().multipartCodecs().encoder(Jackson2JsonEncoder(objectMapper))
        }
        .build()
    private val scheduler = Schedulers.boundedElastic()

    @PostMapping("/trigger")
    fun trigger(
        @RequestBody request: ExecutionRunRequest,
        authentication: Authentication,
    ): Mono<IdResponse> = with(request.projectCoordinates) {
        // Project cannot be taken from executionRequest directly for permission evaluation:
        // it can be fudged by user, who submits it. We should get project from DB based on name/owner combination.
        projectService.findWithPermissionByNameAndOrganization(authentication, projectName, organizationName, Permission.WRITE)
    }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "User ${authentication.username()} doesn't have access to ${request.projectCoordinates}"
        }
        .map { project ->
            executionService.createNew(
                project = project,
                testSuiteIds = request.testSuiteIds,
                files = request.files,
                username = authentication.username(),
                sdk = request.sdk,
                execCmd = request.execCmd,
                batchSizeForAnalyzer = request.batchSizeForAnalyzer
            )
        }
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap { execution ->
            val executionId = execution.requiredId()
            Mono.just(ResponseEntity.accepted().body(executionId))
                .doOnSuccess {
                    blockingToMono {
                        val tests = request.testSuiteIds.flatMap { testService.findTestsByTestSuiteId(it) }
                        log.debug { "Received the following test ids for saving test execution under executionId=$executionId: ${tests.map { it.requiredId() }}" }
                        meterRegistry.timer("save.backend.saveTestExecution").record {
                            testExecutionService.saveTestExecutions(execution, tests)
                        }
                    }
                        .flatMap {
                            initializeAgents(execution)
                                .map {
                                    log.debug { "Initialized agents for execution $executionId" }
                                }
                        }
                        .onErrorResume { ex ->
                            val failReason = "Error during preprocessing. Reason: ${ex.message}"
                            log.error(
                                "$failReason, will mark execution.id=$executionId as failed; error: ",
                                ex
                            )
                            val executionUpdateDto = ExecutionUpdateDto(
                                executionId,
                                ExecutionStatus.ERROR,
                                failReason
                            )
                            blockingToMono {
                                executionService.updateExecutionStatus(execution, executionUpdateDto.status)
                            }.flatMap {
                                executionInfoStorage.upsertIfRequired(executionUpdateDto)
                            }
                        }
                        .subscribeOn(scheduler)
                        .subscribe()
                }
        }


    /**
     * POST request to orchestrator to initiate its work
     */
    private fun initializeAgents(execution: Execution): Mono<ResponseEntity<HttpStatus>> {
        val bodyBuilder = MultipartBodyBuilder().apply {
            part("execution", execution, MediaType.APPLICATION_JSON)
        }

        return webClientOrchestrator
            .post()
            .uri("/initializeAgents")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .toEntity()
    }

    companion object {
        private val log: Logger = getLogger<RunExecutionController>()
    }
}