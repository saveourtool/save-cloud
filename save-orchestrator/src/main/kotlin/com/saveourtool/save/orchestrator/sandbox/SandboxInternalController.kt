package com.saveourtool.save.orchestrator.sandbox

import com.saveourtool.save.agent.AgentVersion
import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.orchestrator.SANDBOX_PROFILE
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

typealias ByteBufferFluxResponse = ResponseEntity<Flux<ByteBuffer>>
typealias StringResponse = ResponseEntity<String>

/**
 * Sandbox implementation of endpoints which are required for save-agent
 */
@Profile(SANDBOX_PROFILE)
@RestController
@RequestMapping("/sandbox/internal")
class SandboxInternalController {
    /**
     * @param agentVersion
     * @return Mono with empty body
     */
    @PostMapping("/saveAgentVersion")
    fun saveAdditionalData(
        @RequestBody agentVersion: AgentVersion
    ): Mono<Unit> = Mono.empty()

    /**
     * @param testExecutionsDto
     * @return response with text value
     */
    @PostMapping("/saveTestResult")
    fun saveExecutionData(
        @RequestBody testExecutionsDto: List<TestExecutionDto>
    ): Mono<StringResponse> = Mono.empty()

    /**
     * @param executionId
     * @return content of requested snapshot
     */
    @PostMapping("/test-suites-sources/download-snapshot-by-execution-id", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadTestSourceSnapshot(
        @RequestParam executionId: Long
    ): Mono<ByteBufferFluxResponse> = Mono.empty()

    /**
     * @param executionId
     * @param fileKey
     * @return content of requested file
     */
    @PostMapping("/files/download", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(
        @RequestParam executionId: Long,
        @RequestBody fileKey: FileKey,
    ): Mono<ByteBufferFluxResponse> = Mono.empty()

    /**
     * @param version
     * @return content of requested save-cli
     */
    @PostMapping("/files/download-save-cli", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadSaveCli(
        @RequestParam version: String,
    ): Mono<out Resource> = Mono.empty()

    /**
     * @param agentId
     * @param testResultDebugInfo
     * @return [Mono] with count of uploaded bytes
     */
    @PostMapping("/files/debug-info")
    fun saveDebugInfo(
        @RequestParam agentId: String,
        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> = Mono.empty()
}
