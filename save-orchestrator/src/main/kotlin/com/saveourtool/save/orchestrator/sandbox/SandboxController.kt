package com.saveourtool.save.orchestrator.sandbox

import com.saveourtool.save.agent.AgentVersion
import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.domain.TestResultDebugInfo
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

typealias ByteBufferFluxResponse = ResponseEntity<Flux<ByteBuffer>>

@Profile(SANDBOX_PROFILE)
@RestController
@RequestMapping("/sandbox")
class SandboxController {
    /**
     * @param agentVersion
     * @return
     */
    @PostMapping("/internal/saveAgentVersion")
    fun additionalData(
        @RequestBody agentVersion: AgentVersion
    ): Mono<Unit> {
        // do nothing for now
        return Mono.empty()
    }

    /**
     * @param testExecutionsDto
     * @return
     */
    @PostMapping("/internal/saveTestResult")
    fun executionData(
        @RequestBody testExecutionsDto: List<TestExecutionDto>
    ): Mono<ResponseEntity<String>> {
        // do nothing for now
        return Mono.empty()
    }

    /**
     * @param executionId
     * @return
     */
    @PostMapping("/internal/test-suites-sources/download-snapshot-by-execution-id", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun testSourceSnapshot(
        @RequestParam executionId: Long
    ): Mono<ByteBufferFluxResponse> {
        // do nothing for now
        return Mono.empty()
    }

    /**
     * @param version
     * @return
     */
    @PostMapping("/internal/files/download-save-cli", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun saveCliDownload(
        @RequestParam version: String,
    ): Mono<out Resource> {
        // do nothing for now
        return Mono.empty()
    }

    /**
     * @param agentId
     * @param testResultDebugInfo
     * @return
     */
    @PostMapping("/internal/files/debug-info")
    fun debugInfo(@RequestParam agentId: String,
                  @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> {
        // do nothing for now
        return Mono.empty()
    }
}
