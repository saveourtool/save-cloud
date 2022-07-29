package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.*
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

typealias TestSuiteList = List<TestSuite>

/**
 * Controller for [TestSuitesSource]
 */
@RestController
@RequestMapping("/internal/test-suites-source")
class TestSuitesSourceController(
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testSuitesSourceSnapshotStorage: TestSuitesSourceSnapshotStorage,
    private val testSuitesService: TestSuitesService,
    private val organizationService: OrganizationService,
    private val gitService: GitService,
    private val executionService: ExecutionService,
) {
    /**
     * @param organizationName
     * @param name
     * @return [TestSuitesSourceDto] found by provided values or not found exception
     */
    @GetMapping("/{organizationName}/{name}")
    fun findAsDtoByName(
        @PathVariable organizationName: String,
        @PathVariable name: String
    ): Mono<TestSuitesSourceDto> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByName(it).toMono()
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "Organization not found by name $name"
        }
        .flatMap { organization ->
            testSuitesSourceService.findByName(organization, name).toMono()
        }
        .switchIfEmptyToNotFound {
            "TestSuitesSource not found by name $name for organization $organizationName"
        }
        .map { it.toDto() }

    /**
     * Upload snapshot of [TestSuitesSource] with [version]
     *
     * @param organizationName
     * @param name
     * @param version
     * @param creationTime
     * @param contentAsMonoPart
     * @return empty response
     */
    @PostMapping("/{organizationName}/{name}/upload-snapshot", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadSnapshot(
        @PathVariable organizationName: String,
        @PathVariable name: String,
        @RequestParam version: String,
        @RequestParam creationTime: Long,
        @RequestPart("content") contentAsMonoPart: Mono<Part>
    ): Mono<Unit> = findAsDtoByName(organizationName, name)
        .map { TestSuitesSourceSnapshotKey(it, version, creationTime) }
        .flatMap { key ->
            contentAsMonoPart.flatMap { part ->
                val content = part.content().map { it.asByteBuffer() }
                testSuitesSourceSnapshotStorage.upload(key, content).map { writtenBytes ->
                    log.info { "Saved ($writtenBytes bytes) snapshot of ${key.testSuitesSourceName} in ${key.organizationName} with version $version" }
                }
            }
        }

    /**
     * Download snapshot of [TestSuitesSource] with [version]
     *
     * @param organizationName
     * @param name
     * @param version
     * @return resource response
     */
    @PostMapping("/{organizationName}/{name}/download-snapshot", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadSnapshot(
        @PathVariable organizationName: String,
        @PathVariable name: String,
        @RequestParam version: String,
    ): Mono<ByteBufferFluxResponse> = testSuitesSourceSnapshotStorage.findKey(organizationName, name, version)
        .map {
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(testSuitesSourceSnapshotStorage.download(it))
        }
        .onErrorReturn(
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .build()
        )

    /**
     * @param organizationName
     * @param name
     * @param version
     * @return true if storage contains [version] of [TestSuitesSource] identified by provided values
     */
    @GetMapping("/{organizationName}/{name}/contains-snapshot")
    fun containsSnapshot(
        @PathVariable organizationName: String,
        @PathVariable name: String,
        @RequestParam version: String,
    ): Mono<Boolean> = testSuitesSourceSnapshotStorage.doesContain(organizationName, name, version)

    /**
     * @param organizationName
     * @param name
     * @return list of [TestSuitesSourceSnapshotKey] are found by [organizationName] and [name]
     */
    @GetMapping("/{organizationName}/{name}/list-snapshot")
    fun listSnapshotVersions(
        @PathVariable organizationName: String,
        @PathVariable name: String,
    ): Mono<TestSuitesSourceSnapshotKeyList> = testSuitesSourceSnapshotStorage.list(organizationName, name)
        .collectList()

    /**
     * @param organizationName
     * @param gitUrl
     * @param testRootPath
     * @param branch
     * @return existed [TestSuitesSourceDto] is found by provided values or a new one
     */
    @PostMapping("/{organizationName}/get-or-create")
    fun getOrCreate(
        @PathVariable organizationName: String,
        @RequestParam gitUrl: String,
        @RequestParam testRootPath: String,
        @RequestParam branch: String,
    ): Mono<TestSuitesSourceDto> = Mono.just(organizationName)
        .flatMap { organizationService.findByName(it).toMono() }
        .map { organization ->
            organization to gitService.getByOrganizationAndUrl(organization, gitUrl)
        }
        .map { (organization, git) ->
            testSuitesSourceService.getOrCreate(organization, git, testRootPath, branch)
        }
        .map { it.toDto() }

    /**
     * @param organizationName
     * @param name
     * @param version
     * @return list of test suites from snapshot with [version] of [TestSuitesSource] found by [organizationName] and [name]
     */
    @GetMapping("/{organizationName}/{name}/get-test-suites")
    fun getTestSuites(
        @PathVariable organizationName: String,
        @PathVariable name: String,
        @RequestParam version: String,
    ): Mono<TestSuiteList> = Mono.fromCallable {
        testSuitesService.getBySourceAndVersion(
            testSuitesSourceService.getByName(organizationName, name),
            version
        )
    }

    /**
     * @param executionId
     * @return selected [TestSuitesSourceDto] with versions for [com.saveourtool.save.entities.Execution] found by provided values
     */
    @GetMapping("/list-snapshot-by-execution-id")
    fun getByExecutionId(
        @RequestParam executionId: Long
    ): Mono<TestSuitesSourceSnapshotKeyList> = executionService.findExecution(executionId)
        .toMono()
        .switchIfEmptyToNotFound { "Execution (id=$executionId) not found" }
        .flatMap { execution ->
            execution.parseAndGetTestSuiteIds()
                .toMono()
                .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
                    "Execution (id=$executionId) doesn't contain testSuiteIds"
                }
        }
        .flatMapMany { testSuitesIds ->
            testSuitesIds.asSequence()
                .map { testSuiteId ->
                    testSuitesService.findTestSuiteById(testSuiteId)
                        .orNotFound { "TestSuite (id=$testSuiteId) not found" }
                        .toDto()
                }
                .map {
                    Triple(it.source.organizationName, it.source.name, it.version)
                }
                .distinct()
                .toFlux()
                .flatMap { (organizationName, sourceName, version) ->
                    testSuitesSourceSnapshotStorage.findKey(organizationName, sourceName, version)
                        .switchIfEmptyToNotFound {
                            "Not found snapshot key for test suites source $sourceName in $organizationName with version $version"
                        }
                }
        }
        .collectList()

    companion object {
        private val log: Logger = getLogger<TestSuitesSourceService>()
    }
}
