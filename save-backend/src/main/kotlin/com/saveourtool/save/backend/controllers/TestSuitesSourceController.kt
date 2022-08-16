package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.configs.ApiSwaggerSupport
import com.saveourtool.save.backend.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.backend.utils.blockingToMono
import com.saveourtool.save.entities.*
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

typealias TestSuiteList = List<TestSuite>

/**
 * Controller for [TestSuitesSource]
 */
@ApiSwaggerSupport
@RestController
@Tags(
    Tag(name = "test-suites-source"),
)
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
     * @return list of [TestSuitesSourceDto] found by provided values or empty response
     */
    @GetMapping(
        path = [
            "/internal/test-suites-sources/{organizationName}/list",
            "/api/$v1/test-suites-sources/{organizationName}/list",
        ],
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "List test suites source by organization name.",
        description = "List test suites source by organization name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true)
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of test suites sources by organization name.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun list(
        @PathVariable organizationName: String,
    ): Mono<TestSuitesSourceDtoList> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByName(it).toMono()
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "Organization not found by name $organizationName"
        }
        .map { organization ->
            testSuitesSourceService.getAllByOrganization(organization)
                .map { it.toDto() }
        }

    /**
     * @param organizationName
     * @param sourceName
     * @return [TestSuitesSourceDto] found by provided values or not found exception
     */
    @GetMapping(
        path = [
            "/internal/test-suites-sources/{organizationName}/{sourceName}",
            "/api/$v1/test-suites-sources/{organizationName}/{sourceName}",
        ],
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get test suites source by organization name and it's name.",
        description = "Get test suites source by organization name and it's name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of test suites sources by organization name.")
    @ApiResponse(responseCode = "404", description = "Test suites source with such name in organization name was not found.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun findAsDtoByName(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String
    ): Mono<TestSuitesSourceDto> = getTestSuitesSource(organizationName, sourceName)
        .map { it.toDto() }

    /**
     * Upload snapshot of [TestSuitesSource] with [version]
     *
     * @param organizationName
     * @param sourceName
     * @param version
     * @param creationTime
     * @param contentAsMonoPart
     * @return empty response
     */
    @PostMapping(
        path = [
            "/internal/test-suites-sources/{organizationName}/{sourceName}/upload-snapshot",
            "/api/$v1/test-suites-sources/{organizationName}/{sourceName}/upload-snapshot",
        ],
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Upload a snapshot of test suites source.",
        description = "Upload a snapshot of test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
        Parameter(name = "version", `in` = ParameterIn.QUERY, description = "version of uploading snapshot", required = true),
        Parameter(name = "creationTime", `in` = ParameterIn.QUERY, description = "creationTime of uploading snapshot", required = true),
        Parameter(name = "content", `in` = ParameterIn.DEFAULT, description = "content of uploading snapshot", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully uploaded provided snapshot.")
    @ApiResponse(responseCode = "404", description = "Test suites source with such name in organization name was not found.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun uploadSnapshot(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
        @RequestParam creationTime: Long,
        @RequestPart("content") contentAsMonoPart: Mono<Part>
    ): Mono<Unit> = findAsDtoByName(organizationName, sourceName)
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
     * @param sourceName
     * @param version
     * @return resource response
     */
    @PostMapping(
        path = [
            "/internal/test-suites-sources/{organizationName}/{sourceName}/download-snapshot",
            "/api/$v1/test-suites-sources/{organizationName}/{sourceName}/download-snapshot",
        ],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Download a snapshot of test suites source.",
        description = "Download a snapshot of test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
        Parameter(name = "version", `in` = ParameterIn.QUERY, description = "version of downloading snapshot", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully downloaded snapshot with provided version.")
    @ApiResponse(responseCode = "404", description = "Test suites source with such name in organization name was not found.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun downloadSnapshot(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
    ): Mono<ByteBufferFluxResponse> = findAsDtoByName(organizationName, sourceName)
        .flatMap {
            it.downloadSnapshot(version)
        }

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return true if storage contains [version] of [TestSuitesSource] identified by provided values
     */
    @GetMapping(
        path = [
            "/internal/test-suites-sources/{organizationName}/{sourceName}/contains-snapshot",
            "/api/$v1/test-suites-sources/{organizationName}/{sourceName}/contains-snapshot",
        ],
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Check that test suites source contains provided version.",
        description = "Check that test suites source contains provided version.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
        Parameter(name = "version", `in` = ParameterIn.QUERY, description = "version of checking snapshot", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully checked snapshot with provided values.")
    @ApiResponse(responseCode = "404", description = "Test suites source with such name in organization name was not found.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun containsSnapshot(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
    ): Mono<Boolean> = findAsDtoByName(organizationName, sourceName)
        .flatMap {
            testSuitesSourceSnapshotStorage.doesContain(it.organizationName, it.name, version)
        }

    /**
     * @param organizationName
     * @param sourceName
     * @return list of [TestSuitesSourceSnapshotKey] are found by [organizationName] and [sourceName]
     */
    @GetMapping(
        path = [
            "/internal/test-suites-sources/{organizationName}/{sourceName}/list-snapshot",
            "/api/$v1/test-suites-sources/{organizationName}/{sourceName}/list-snapshot",
        ],
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "List of snapshot for test suites source.",
        description = "List of snapshot for test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed snapshots for requested test suites source.")
    @ApiResponse(responseCode = "404", description = "Test suites source with such name in organization name was not found.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun listSnapshotVersions(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
    ): Mono<TestSuitesSourceSnapshotKeyList> = findAsDtoByName(organizationName, sourceName)
        .flatMap {
            testSuitesSourceSnapshotStorage.list(it.organizationName, it.name)
                .collectList()
        }

    /**
     * @param organizationName
     * @return list of [TestSuitesSourceSnapshotKey] are found by [organizationName]
     */
    @GetMapping(path = [
        "/internal/test-suites-sources/{organizationName}/list-snapshot",
        "/api/$v1/test-suites-sources/{organizationName}/list-snapshot",
    ],
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "List of snapshot for all test suites sources in requested organization.",
        description = "List of snapshot for all test suites sources in requested organization.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed snapshots for all test suites sources in requested organization.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun listSnapshots(
        @PathVariable organizationName: String,
    ): Mono<TestSuitesSourceSnapshotKeyList> = getOrganization(organizationName)
        .flatMap { organization ->
            testSuitesSourceSnapshotStorage.list()
                .filter { it.organizationName == organization.name }
                .collectList()
        }

    /**
     * @param organizationName
     * @param gitUrl
     * @param testRootPath
     * @param branch
     * @return existed [TestSuitesSourceDto] is found by provided values or a new one
     */
    @PostMapping(path = [
        "/internal/test-suites-sources/{organizationName}/get-or-create",
        "/api/$v1/test-suites-sources/{organizationName}/get-or-create",
    ],
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get or create a new test suite source by provided values.",
        description = "Get or create a new test suite source by provided values.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.QUERY, description = "name of organization", required = true),
        Parameter(name = "gitUrl", `in` = ParameterIn.QUERY, description = "git url of test suites source", required = true),
        Parameter(name = "testRootPath", `in` = ParameterIn.QUERY, description = "test root path of test suites source", required = true),
        Parameter(name = "branch", `in` = ParameterIn.QUERY, description = "branch of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully get or create test suites source with requested values.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    @ApiResponse(responseCode = "409", description = "Git credentials was not found by provided url.")
    fun getOrCreate(
        @PathVariable organizationName: String,
        @RequestParam gitUrl: String,
        @RequestParam testRootPath: String,
        @RequestParam branch: String,
    ): Mono<TestSuitesSourceDto> = getOrganization(organizationName)
        .zipWhen { organization ->
            gitService.findByOrganizationAndUrl(organization, gitUrl)
                .toMono()
                .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
                    "There is no git credential with url $gitUrl in $organizationName"
                }
        }
        .map { (organization, git) ->
            testSuitesSourceService.getOrCreate(organization, git, testRootPath, branch)
        }
        .map { it.toDto() }

    /**
     * @param executionId
     * @return selected [TestSuitesSourceDto] with versions for [com.saveourtool.save.entities.Execution] found by provided values
     */
    @PostMapping("/internal/test-suites-sources/download-snapshot-by-execution-id", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Download a snapshot of test suites source which is assigned to execution.",
        description = "Download a snapshot of test suites source which is assigned to execution.",
    )
    @Parameters(
        Parameter(name = "executionId", `in` = ParameterIn.QUERY, description = "ID of execution", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully downloaded snapshot of test suites source for requested execution.")
    @ApiResponse(responseCode = "404", description = "Execution was not found by provided ID.")
    @ApiResponse(responseCode = "409", description = "IDs of test suites were not set on requested execution or weren't found by ID.")
    @ApiResponse(responseCode = "404", description = "Test suites source with such name in organization name was not found.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun downloadByExecutionId(
        @RequestParam executionId: Long
    ): Mono<ByteBufferFluxResponse> = blockingToMono {
        val execution = executionService.findExecution(executionId)
            .orNotFound { "Execution (id=$executionId) not found" }
        val testSuiteId = execution.parseAndGetTestSuiteIds()?.firstOrNull()
            .orConflict { "Execution (id=$executionId) doesn't contain testSuiteIds" }
        testSuitesService.findTestSuiteById(testSuiteId)
            .orConflict { "TestSuite (id=$testSuiteId) not found" }
            .toDto()
            .let { it.source to it.version }
    }.flatMap { (source, version) ->
        source.downloadSnapshot(version)
    }

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return list of test suites from snapshot with [version] of [TestSuitesSource] found by [organizationName] and [sourceName]
     */
    @GetMapping("/internal/test-suites-sources/{organizationName}/{sourceName}/get-test-suites")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "List of test suites in requested test suites source.",
        description = "List of test suites in requested test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed snapshots for requested test suites source.")
    @ApiResponse(responseCode = "404", description = "Test suites source with such name in organization name was not found.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun getTestSuites(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
    ): Mono<TestSuiteList> = getTestSuitesSource(organizationName, sourceName)
        .map { testSuitesSource ->
            testSuitesService.getBySourceAndVersion(
                testSuitesSource,
                version
            )
        }

    @GetMapping("/api/$v1/test-suites-sources/{organizationName}/{sourceName}/get-test-suites")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "List of test suites in requested test suites source.",
        description = "List of test suites in requested test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed snapshots for requested test suites source.")
    @ApiResponse(responseCode = "404", description = "Test suites source with such name in organization name was not found.")
    @ApiResponse(responseCode = "409", description = "Organization was not found by provided name.")
    fun getTestSuiteDtos(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
    ): Mono<List<TestSuiteDto>> = getTestSuitesSource(organizationName, sourceName)
        .map { testSuitesSource ->
            testSuitesService.getBySourceAndVersion(
                testSuitesSource,
                version
            ).map {
                it.toDto(it.requiredId())
            }
        }

    /**
     * Will be removed in phase 3
     *
     * @return list of standard test suites sources
     */
    @GetMapping(
        path = [
            "/internal/test-suites-sources/get-standard",
            "/api/$v1/test-suites-sources/get-standard",
        ],
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "List of standard test suites sources.",
        description = "List of standard test suites sources.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed standard test suites sources.")
    fun getStandardTestSuitesSources(): Mono<TestSuitesSourceDtoList> = Mono.fromCallable {
        testSuitesSourceService.getStandardTestSuitesSources()
            .map { it.toDto() }
    }

    private fun getOrganization(organizationName: String): Mono<Organization> = Mono.just(organizationName)
        .flatMap {
            organizationService.findByName(it).toMono()
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
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

    @GetMapping("/api/$v1/test-suites-sources/organizations-list")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get organizations with public test suite sources.",
        description = "Get list of organizations with public test suite sources",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched organizations with public test suite sources.")
    fun getOrganizationNamesWithPublicTestSuiteSources(
        authentication: Authentication,
    ): Mono<List<String>> = testSuitesSourceService.getOrganizationsWithPublicTestSuiteSources().toMono()

    @PostMapping("/api/$v1/test-suites-sources/{organizationName}/{sourceName}/fetch")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Post fetching of new tests from test suites source.",
        description = "Post fetching of new tests from test suites source.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully trigger fetching new tests from requested test suites source.")
    fun triggerFetch(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono { testSuitesSourceService.findByName(organizationName, sourceName) }
        .flatMap { testSuitesSource ->
            Mono.just(
                ResponseEntity.ok()
                    .body("Trigger fetching new tests from $sourceName in $organizationName")
            ).doOnSuccess {
                testSuitesSourceService.fetch(testSuitesSource.toDto())
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe()
            }
        }

    private fun TestSuitesSourceDto.downloadSnapshot(
        version: String
    ): Mono<ByteBufferFluxResponse> = testSuitesSourceSnapshotStorage.findKey(organizationName, name, version)
        .switchIfEmptyToNotFound {
            "Not found a snapshot of $name in $organizationName with version=$version"
        }
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

    companion object {
        private val log: Logger = getLogger<TestSuitesSourceService>()
    }
}
