package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.TestsSourceSnapshotStorage
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.test.TestsSourceVersionDto
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.*

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller for [TestSuitesSource]
 */
@RestController
@RequestMapping("/internal/test-suites-sources")
class TestSuitesSourceInternalController(
    private val testsSourceVersionService: TestsSourceVersionService,
    private val snapshotStorage: TestsSourceSnapshotStorage,
    private val testSuitesService: TestSuitesService,
    private val testSuitesSourceService: TestSuitesSourceService,
    private val executionService: ExecutionService,
    private val lnkExecutionTestSuiteService: LnkExecutionTestSuiteService,
) {
    /**
     * @param snapshotDto
     * @param contentAsMonoPart
     * @return [Mono] with updated [snapshotDto]
     */
    @PostMapping("/upload-snapshot", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadSnapshot(
        @RequestPart("snapshot") snapshotDto: TestsSourceSnapshotDto,
        @RequestPart("content") contentAsMonoPart: Mono<Part>,
    ): Mono<TestsSourceSnapshotDto> = contentAsMonoPart.flatMap { part ->
        val content = part.content().map { it.asByteBuffer() }
        snapshotStorage.uploadAndReturnUpdatedKey(snapshotDto, content)
    }

    /**
     * @param versionDto
     * @return [Mono] without value
     */
    @PostMapping("/save-version")
    fun saveVersion(
        @RequestBody versionDto: TestsSourceVersionDto,
    ): Mono<Unit> = blockingToMono {
        testsSourceVersionService.save(versionDto)
    }

    /**
     * @param snapshotDto
     * @return [Mono] with result or empty
     */
    @PostMapping("/find-snapshot")
    fun findSnapshot(
        @RequestBody snapshotDto: TestsSourceSnapshotDto,
    ): Mono<TestsSourceSnapshotDto> = blockingToMono {
        testsSourceVersionService.findSnapshot(snapshotDto)
    }

    /**
     * @param executionId
     * @return content of tests related to provided values
     */
    @PostMapping("/download-snapshot-by-execution-id", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadByExecutionId(
        @RequestParam executionId: Long
    ): Mono<ByteBufferFluxResponse> = blockingToMono {
        val execution = executionService.findExecution(executionId)
            .orNotFound { "Execution (id=$executionId) not found" }
        val testSuite = lnkExecutionTestSuiteService.getAllTestSuitesByExecution(execution).firstOrNull().orNotFound {
            "Execution (id=$executionId) doesn't have any testSuites"
        }
        testSuite
            .toDto()
            .let { it.source to it.version }
    }.flatMap { (source, version) ->
        source.downloadSnapshot(version)
    }

    private fun TestSuitesSourceDto.downloadSnapshot(
        version: String
    ): Mono<ByteBufferFluxResponse> = blockingToMono {
        testsSourceVersionService.findSnapshot(organizationName, name, version)
    }
        .switchIfEmptyToNotFound {
            "Not found a snapshot of $name in $organizationName with version=$version"
        }
        .map { snapshot ->
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(snapshotStorage.download(snapshot))
        }
}
