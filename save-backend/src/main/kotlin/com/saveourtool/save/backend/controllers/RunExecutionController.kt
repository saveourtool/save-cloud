package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.EmptyResponse
import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.ExecutionInfoStorage
import com.saveourtool.save.backend.utils.blockingToMono
import com.saveourtool.save.backend.utils.username
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.ExecutionRunRequest
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionUpdateDto
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import com.saveourtool.save.utils.switchIfEmptyToResponseException
import com.saveourtool.save.v1

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * Controller for running execution
 */
@RestController
@RequestMapping("/api/$v1/run")
@Suppress("LongParameterList")
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

    /**
     * @param request incoming request from frontend
     * @param authentication
     * @return response with ID of created [Execution]
     */
    @PostMapping("/trigger")
    fun trigger(
        @RequestBody request: ExecutionRunRequest,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.just(request.projectCoordinates)
        .validateAccess(authentication) { it }
        .map {
            executionService.createNew(
                projectCoordinates = request.projectCoordinates,
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
            Mono.just(execution.toAcceptedResponse())
                .doOnSuccess {
                    asyncTrigger(execution)
                }
        }

    /**
     * @param executionId ID of [Execution] which needs to be copied for new execution
     * @param authentication
     * @return response with ID of new created [Execution]
     */
    @PostMapping("/reTrigger")
    fun reTrigger(
        @RequestParam executionId: Long,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono { executionService.findExecution(executionId) }
        .switchIfEmptyToNotFound { "Not found execution id = $executionId" }
        .validateAccess(authentication) { execution ->
            ProjectCoordinates(
                execution.project.organization.name,
                execution.project.name
            )
        }
        .map { executionService.createNewCopy(it, authentication.username()) }
        .flatMap { execution ->
            Mono.just(execution.toAcceptedResponse())
                .doOnSuccess {
                    asyncTrigger(execution)
                }
        }

    private fun <T> Mono<T>.validateAccess(
        authentication: Authentication,
        projectCoordinatesGetter: (T) -> ProjectCoordinates,
    ): Mono<T> =
            flatMap { value ->
                val projectCoordinates = projectCoordinatesGetter(value)
                with(projectCoordinates) {
                    projectService.findWithPermissionByNameAndOrganization(
                        authentication,
                        projectName,
                        organizationName,
                        Permission.WRITE
                    )
                }.switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
                    "User ${authentication.username()} doesn't have access to $projectCoordinates"
                }.map { value }
            }

    @Suppress("TOO_LONG_FUNCTION")
    private fun asyncTrigger(execution: Execution) {
        val executionId = execution.requiredId()
        blockingToMono {
            val tests = execution.parseAndGetTestSuiteIds()
                .orEmpty()
                .flatMap { testService.findTestsByTestSuiteId(it) }
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

    /**
     * POST request to orchestrator to initiate its work
     */
    private fun initializeAgents(execution: Execution): Mono<EmptyResponse> {
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

    private fun Execution.toAcceptedResponse(): StringResponse =
            ResponseEntity.accepted().body("Clone pending, execution id is ${requiredId()}")

    companion object {
        private val log: Logger = getLogger<RunExecutionController>()
    }
}
