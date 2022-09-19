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
@RequestMapping("/sandbox")
class SandboxController {
    /**
     * @param agentVersion
     * @return Mono with empty body
     */
    @PostMapping("/internal/saveAgentVersion")
    fun saveAdditionalData(
        @RequestBody agentVersion: AgentVersion
    ): Mono<Unit> {
        // do nothing for now
        return Mono.empty()
    }

    /**
     * @param testExecutionsDto
     * @return response with text value
     */
    @PostMapping("/internal/saveTestResult")
    fun saveExecutionData(
        @RequestBody testExecutionsDto: List<TestExecutionDto>
    ): Mono<StringResponse> {
        // do nothing for now
        return Mono.empty()
    }

    /**
     * @param executionId
     * @return content of requested snapshot
     */
    @PostMapping("/internal/test-suites-sources/download-snapshot-by-execution-id", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadTestSourceSnapshot(
        @RequestParam executionId: Long
    ): Mono<ByteBufferFluxResponse> {
        // do nothing for now
        return Mono.empty()
    }

    /**
     * @param executionId
     * @param fileKey
     * @return content of requested file
     */
    @PostMapping("/internal/files/download", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(
        @RequestParam executionId: Long,
        @RequestBody fileKey: FileKey,
    ): Mono<ByteBufferFluxResponse> {
        // do nothing for now
        return Mono.empty()
    }

    /**
     * @param version
     * @return content of requested save-cli
     */
    @PostMapping("/internal/files/download-save-cli", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadSaveCli(
        @RequestParam version: String,
    ): Mono<out Resource> {
        // do nothing for now
        return Mono.empty()
    }

    /**
     * @param agentId
     * @param testResultDebugInfo
     * @return [Mono] with count of uploaded bytes
     */
    @PostMapping("/internal/files/debug-info")
    fun saveDebugInfo(
        @RequestParam agentId: String,
        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> {
        // do nothing for now
        return Mono.empty()
    }
}
