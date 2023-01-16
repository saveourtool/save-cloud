package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.utils.toResponseEntity
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.domain.EntitySaveStatus
import com.saveourtool.save.entities.*
import com.saveourtool.save.entities.TestSuitesSource.Companion.toTestSuiteSource
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.test.TestsSourceVersionInfoList
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

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

typealias EntitySaveStatusResponse = ResponseEntity<EntitySaveStatus>
typealias StringListResponse = ResponseEntity<List<String>>

/**
 * Controller for [TestSuitesSource]
 */
@ApiSwaggerSupport
@RestController
@Tags(
    Tag(name = "test-suites-source"),
)
@Suppress("LongParameterList")
class TestSuitesSourceController(
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testsSourceVersionService: TestsSourceVersionService,
    private val testSuitesService: TestSuitesService,
    private val organizationService: OrganizationService,
    private val gitService: GitService,
    private val executionService: ExecutionService,
    private val lnkExecutionTestSuiteService: LnkExecutionTestSuiteService,
) {
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
    @ApiResponse(responseCode = "404", description = "Organization was not found by provided name.")
    fun list(
        @PathVariable organizationName: String,
    ): Mono<TestSuitesSourceDtoList> = getOrganization(organizationName)
        .map { organization ->
            testSuitesSourceService.getAllByOrganization(organization)
                .map { it.toDto() }
        }

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
        summary = "Get test suites source by organization name and source name.",
        description = "Get test suites source by organization name and test suites source name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of test suites sources by organization name.")
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
    fun findAsDtoByName(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String
    ): Mono<TestSuitesSourceDto> = getTestSuitesSource(organizationName, sourceName)
        .map { it.toDto() }

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
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    fun uploadSnapshot(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
        @RequestParam creationTime: Long,
        @RequestPart("content") contentAsMonoPart: Mono<Part>
    ): Mono<Unit> = findAsDtoByName(organizationName, sourceName)
        .map {
            val parsedCreationTime = creationTime.millisToInstant().toLocalDateTime(TimeZone.UTC)
            TestsSourceVersionInfo(
                organizationName = it.organizationName,
                sourceName = it.name,
                version = version,
                creationTime = parsedCreationTime,
                commitId = version,
                commitTime = parsedCreationTime,
            )
        }
        .flatMap { key ->
            contentAsMonoPart.flatMap { part ->
                val content = part.content().map { it.asByteBuffer() }
                testsSourceVersionService.upload(key, content).map { writtenBytes ->
                    log.info { "Saved ($writtenBytes bytes) snapshot of ${key.sourceName} in ${key.organizationName} with version $version" }
                }
            }
        }

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
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
    fun downloadSnapshot(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
    ): Mono<ByteBufferFluxResponse> = findAsDtoByName(organizationName, sourceName)
        .flatMap {
            it.downloadSnapshot(version)
        }

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
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
    fun containsSnapshot(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
    ): Mono<Boolean> = findAsDtoByName(organizationName, sourceName)
        .flatMap {
            testsSourceVersionService.doesContain(it.organizationName, it.name, version)
        }

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
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
    @ApiResponse(responseCode = "404", description = ".")
    fun listSnapshotVersions(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
    ): Mono<TestsSourceVersionInfoList> = findAsDtoByName(organizationName, sourceName)
        .flatMap {
            testsSourceVersionService.list(it.organizationName, it.name)
                .collectList()
        }

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
    @ApiResponse(responseCode = "404", description = "Organization was not found by provided name.")
    fun listSnapshots(
        @PathVariable organizationName: String,
    ): Mono<TestsSourceVersionInfoList> = getOrganization(organizationName)
        .flatMap { organization ->
            testsSourceVersionService.list(organization.name)
                .collectList()
        }

    @PostMapping("/api/$v1/test-suites-sources/create")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get or create a new test suite source by provided values.",
        description = "Get or create a new test suite source by provided values.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully get or create test suites source with requested values.")
    @ApiResponse(responseCode = "404", description = "Either git credentials were not found by provided url or organization was not found by provided name.")
    @ApiResponse(responseCode = "409", description = "Test suite name is already taken.")
    fun createTestSuitesSource(
        @RequestBody testSuiteRequest: TestSuitesSourceDto,
    ): Mono<EntitySaveStatusResponse> = getOrganization(testSuiteRequest.organizationName)
        .zipWhen { getGit(it, testSuiteRequest.gitDto.url) }
        .map { (organization, git) ->
            testSuiteRequest.toTestSuiteSource(organization, git)
        }
        .flatMap { testSuitesSource ->
            when (val saveStatus = testSuitesSourceService.createSourceIfNotPresent(testSuitesSource)) {
                EntitySaveStatus.EXIST, EntitySaveStatus.CONFLICT, EntitySaveStatus.NEW -> Mono.just(saveStatus.toResponseEntity())
                else -> Mono.error(IllegalStateException("Not expected status for creating a new entity"))
            }
        }

    @PostMapping("/api/$v1/test-suites-sources/update")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get or create a new test suite source by provided values.",
        description = "Get or create a new test suite source by provided values.",
    )
    @Parameters(
        Parameter(name = "id", `in` = ParameterIn.QUERY, description = "ID of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully get or create test suites source with requested values.")
    @ApiResponse(responseCode = "400", description = "Try to change organization or git by this request.")
    @ApiResponse(responseCode = "404", description = "Test suites source was not found by provided ID.")
    @ApiResponse(responseCode = "409", description = "Test suite name is already taken.")
    fun update(
        @RequestParam("id") id: Long,
        @RequestBody dtoToUpdate: TestSuitesSourceDto
    ): Mono<EntitySaveStatusResponse> = getTestSuitesSource(id)
        .requireOrSwitchToResponseException({ organization.name == dtoToUpdate.organizationName }, HttpStatus.BAD_REQUEST) {
            "Organization cannot be changed in TestSuitesSource"
        }
        .requireOrSwitchToResponseException({ git.url == dtoToUpdate.gitDto.url }, HttpStatus.BAD_REQUEST) {
            "Git cannot be changed in TestSuitesSource"
        }
        .map { originalEntity ->
            originalEntity.name to originalEntity.apply {
                name = dtoToUpdate.name
                description = dtoToUpdate.description
                testRootPath = dtoToUpdate.testRootPath
                latestFetchedVersion = dtoToUpdate.latestFetchedVersion
            }
        }
        .flatMap { (originalName, updatedEntity) ->
            when (val saveStatus = testSuitesSourceService.update(updatedEntity)) {
                EntitySaveStatus.EXIST, EntitySaveStatus.CONFLICT -> Mono.just(saveStatus.toResponseEntity())
                EntitySaveStatus.UPDATED -> {
                    val newName = updatedEntity.name
                    val movingSnapshots = if (originalName != newName) {
                        testsSourceVersionService.updateSourceName(updatedEntity.organization.name, originalName, newName)
                    } else {
                        Mono.just(Unit)
                    }
                    movingSnapshots.then(Mono.just(saveStatus.toResponseEntity()))
                }
                else -> Mono.error(IllegalStateException("Not expected status for creating a new entity"))
            }
        }

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
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found" +
                " or test suites were not found by execution's ids.",
    )
    @ApiResponse(responseCode = "409", description = "IDs of test suites were not set on requested execution.")
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
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
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
                it.toDto()
            }
        }

    @DeleteMapping(
        "/api/$v1/test-suites-sources/{organizationName}/{sourceName}/delete-test-suites-and-snapshot",
        "/internal/test-suites-sources/{organizationName}/{sourceName}/delete-test-suites-and-snapshot"
    )
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "DELETE",
        summary = "Delete test suites and snapshot for requested version from provided test suites source.",
        description = "Delete test suites and snapshot for requested version from provided test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted test suites and snapshot for requested version from provided source.")
    @ApiResponse(
        responseCode = "404",
        description = "Either organization was not found by provided name or test suites source with such name in organization name was not found.",
    )
    fun deleteTestSuitesAndSnapshot(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam version: String,
    ): Mono<Boolean> = getTestSuitesSource(organizationName, sourceName)
        .map {
            testSuitesService.deleteTestSuite(it, version)
        }
        .then(testsSourceVersionService.delete(organizationName, sourceName, version))

    private fun getOrganization(organizationName: String): Mono<Organization> = blockingToMono {
        organizationService.findByNameAndCreatedStatus(organizationName)
    }.switchIfEmptyToNotFound {
        "Organization not found by name $organizationName"
    }

    private fun getGit(organization: Organization, gitUrl: String): Mono<Git> = blockingToMono {
        gitService.findByOrganizationAndUrl(organization, gitUrl)
    }.switchIfEmptyToNotFound {
        "There is no git credential with url $gitUrl in ${organization.name}"
    }

    private fun getTestSuitesSource(organizationName: String, name: String): Mono<TestSuitesSource> =
            getOrganization(organizationName)
                .flatMap { organization ->
                    testSuitesSourceService.findByName(organization, name).toMono()
                }
                .switchIfEmptyToNotFound {
                    "TestSuitesSource not found by name $name for organization $organizationName"
                }

    private fun getTestSuitesSource(id: Long): Mono<TestSuitesSource> =
            blockingToMono {
                testSuitesSourceService.findById(id)
            }
                .switchIfEmptyToNotFound {
                    "TestSuiteSource not found by ID $id"
                }

    @PostMapping("/api/$v1/test-suites-sources/{organizationName}/{sourceName}/fetch")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Post fetching of new tests from test suites source.",
        description = "Post fetching of new tests from test suites source.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of organization", required = true),
        Parameter(name = "sourceName", `in` = ParameterIn.PATH, description = "name of test suites source", required = true),
        Parameter(name = "mode", `in` = ParameterIn.QUERY, description = "fetch mode", required = true),
        Parameter(name = "version", `in` = ParameterIn.QUERY, description = "version to be fetched: tag, branch or commit id", required = true),
    )
    @ApiResponse(responseCode = "202", description = "Successfully trigger fetching new tests from requested test suites source.")
    fun triggerFetch(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        @RequestParam mode: TestSuitesSourceFetchMode,
        @RequestParam version: String,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono { testSuitesSourceService.findByName(organizationName, sourceName) }
        .flatMap { testSuitesSource ->
            Mono.just(
                ResponseEntity.accepted()
                    .body("Trigger fetching new tests from $sourceName in $organizationName")
            ).doOnSuccess {
                testSuitesSourceService.fetch(testSuitesSource.toDto(), mode, version)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe()
            }
        }

    @GetMapping("/api/$v1/test-suites-sources/{organizationName}/{sourceName}/tag-list-to-fetch")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get list of tags which can be fetched from test suites source.",
        description = "Get list of tags which can be fetched from test suites source.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed tags which can be fetched from requested test suites source.")
    fun tagListToFetch(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        authentication: Authentication,
    ): Mono<StringListResponse> = blockingToMono { testSuitesSourceService.findByName(organizationName, sourceName) }
        .flatMap { testSuitesSourceService.tagList(it.toDto()) }
        .zipWith(testsSourceVersionService.list(organizationName, sourceName)
            .map { it.version }
            .collectList())
        .map { (tags, versions) ->
            ResponseEntity.ok()
                .body(tags.filterNot { it in versions })
        }

    @GetMapping("/api/$v1/test-suites-sources/{organizationName}/{sourceName}/branch-list-to-fetch")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get list of branches which can be fetched from test suites source.",
        description = "Get list of branches which can be fetched from test suites source.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully listed branches which can be fetched from requested test suites source.")
    fun branchListToFetch(
        @PathVariable organizationName: String,
        @PathVariable sourceName: String,
        authentication: Authentication,
    ): Mono<StringListResponse> = blockingToMono { testSuitesSourceService.findByName(organizationName, sourceName) }
        .flatMap { testSuitesSourceService.branchList(it.toDto()) }
        .map { ResponseEntity.ok().body(it) }

    @GetMapping("/api/$v1/test-suites-sources/available")
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
    ): Mono<TestSuitesSourceDtoList> = testSuitesSourceService.getAvailableTestSuiteSources().toMono()
        .map {testSuitesSourceList ->
            testSuitesSourceList.map { it.toDto() }
        }

    private fun TestSuitesSourceDto.downloadSnapshot(
        version: String
    ): Mono<ByteBufferFluxResponse> = testsSourceVersionService.doesContain(organizationName, name, version)
        .filter { it }
        .switchIfEmptyToNotFound {
            "Not found a snapshot of $name in $organizationName with version=$version"
        }
        .map {
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(testsSourceVersionService.download(organizationName, name, version))
        }

    companion object {
        private val log: Logger = getLogger<TestSuitesSourceService>()
    }
}
