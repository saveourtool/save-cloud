package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * Controller for [TestSuitesSource]
 */
@RestController
@RequestMapping("/internal/test-suites-sources")
class TestSuitesSourceInternalController(
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testSuitesSourceVersionService: TestsSourceVersionService,
    private val organizationService: OrganizationService,
    private val executionService: ExecutionService,
    private val lnkExecutionTestSuiteService: LnkExecutionTestSuiteService,
) {
    /**
     * @param versionInfo
     * @param contentAsMonoPart
     * @return [Mono] without value
     */
    @PostMapping("/upload-snapshot", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadSnapshot(
        @RequestPart("versionInfo") versionInfo: TestsSourceVersionInfo,
        @RequestPart("content") contentAsMonoPart: Mono<Part>,
    ): Mono<Unit> = contentAsMonoPart.flatMap { part ->
        val content = part.content().map { it.asByteBuffer() }
        testSuitesSourceVersionService.upload(versionInfo, content).map { writtenBytes ->
            log.info { "Saved ($writtenBytes bytes) snapshot of ${versionInfo.sourceName} in ${versionInfo.organizationName} with version ${versionInfo.version}" }
        }
    }

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return [Mono] with result
     */
    @GetMapping("/{organizationName}/{sourceName}/contains-snapshot")
    fun containsSnapshot(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
    ): Mono<Boolean> = getTestSuitesSource(organizationName, sourceName)
        .flatMap {
            testSuitesSourceVersionService.doesContain(it.organization.name, it.name, version)
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

    private fun getOrganization(organizationName: String): Mono<Organization> = blockingToMono {
        organizationService.findByNameAndCreatedStatus(organizationName)
    }.switchIfEmptyToNotFound {
        "Organization not found by name $organizationName"
    }

    private fun getTestSuitesSource(organizationName: String, name: String): Mono<TestSuitesSource> =
            getOrganization(organizationName)
                .flatMap { organization ->
                    testSuitesSourceService.findByName(organization, name).toMono()
                }
                .switchIfEmptyToNotFound {
                    "TestSuitesSource not found by name $name for organization $organizationName"
                }

    private fun TestSuitesSourceDto.downloadSnapshot(
        version: String
    ): Mono<ByteBufferFluxResponse> = testSuitesSourceVersionService.doesContain(organizationName, name, version)
        .filter { it }
        .switchIfEmptyToNotFound {
            "Not found a snapshot of $name in $organizationName with version=$version"
        }
        .map {
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(testSuitesSourceVersionService.download(organizationName, name, version))
        }

    companion object {
        private val log: Logger = getLogger<TestSuitesSourceService>()
    }
}
