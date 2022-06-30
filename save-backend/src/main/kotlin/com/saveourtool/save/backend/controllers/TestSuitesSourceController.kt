package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.DataBufferFluxResponse
import com.saveourtool.save.backend.EmptyResponse
import com.saveourtool.save.backend.service.TestSuitesService
import com.saveourtool.save.backend.service.TestSuitesSourceLogService
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.backend.storage.TestSuitesSourceStorage
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestSuitesSourceLog
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.Part
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller for test suites source [TestSuitesSourceDto]
 */
@RestController
class TestSuitesSourceController(
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testSuitesService: TestSuitesService,
    private val testSuitesSourceLogService: TestSuitesSourceLogService,
    private val testSuitesSourceStorage: TestSuitesSourceStorage
) {
    /**
     * @param name name of [TestSuitesSource]
     * @return entity of [TestSuitesSource] or null
     */
    @PostMapping("/internal/test-suites-source/find-by-name")
    fun findByName(@RequestParam name: String): ResponseEntity<TestSuitesSource?> =
            ResponseEntity.ok().body(testSuitesSourceService.findByName(name))

    /**
     * @param dto entity as DTO [TestSuitesSourceDto]
     * @return saved entity as [TestSuitesSource]
     */
    @PostMapping("/internal/test-suites-source/register-new")
    fun createNew(@RequestBody dto: TestSuitesSourceDto): ResponseEntity<TestSuitesSource> =
            ResponseEntity.ok().body(testSuitesSourceService.createNew(dto))

    /**
     * @param testSuitesSourceDto DTO of [TestSuitesSourceDto]
     * @param version version to be found
     * @return found instance [TestSuitesSourceLog] or null
     */
    @PostMapping("/internal/test-suites-source/find-version")
    fun findLogWithVersion(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam version: String
    ): ResponseEntity<TestSuitesSourceLog?> =
            ResponseEntity.ok().body(testSuitesSourceLogService.findByVersion(testSuitesSourceDto, version))

    /**
     * @param testSuitesSourceDto DTO of [TestSuitesSourceDto]
     * @param version version to be checked
     * @return if there is a log with provided version -- true, otherwise false
     */
    @PostMapping("/internal/test-suites-source/check-version")
    fun containsLogWithVersion(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam version: String
    ): ResponseEntity<Boolean> =
            ResponseEntity.ok().body(testSuitesSourceLogService.containsVersion(testSuitesSourceDto, version))

    /**
     * @param testSuitesSourceDto DTO of [TestSuitesSourceDto]
     * @param version a new version
     * @return a new instance of [TestSuitesSourceLog]
     */
    @PostMapping("/internal/test-suites-source/log-new-version")
    fun createNewLog(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam version: String
    ): ResponseEntity<TestSuitesSourceLog> =
            ResponseEntity.ok().body(testSuitesSourceLogService.createNew(testSuitesSourceDto, version))

    /**
     * @param testSuiteIds list of ID of [com.saveourtool.save.entities.TestSuite]
     * @return a single instance of [TestSuitesSource]
     */
    @PostMapping("/internal/test-suites-source/get-single-by-test-suites-ids")
    fun getSingleByTestSuiteIds(@RequestBody testSuiteIds: List<Long>): ResponseEntity<TestSuitesSource> =
            ResponseEntity.ok().body(testSuitesService.getSingleByTestSuiteIds(testSuiteIds))

    /**
     * @param projectCoordinates
     * @param testSuitesSourceLog
     */
    @PostMapping(
        path = ["/internal/test-suites-source/download"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    fun download(
        @RequestPart("projectCoordinates", required = false) projectCoordinates: ProjectCoordinates?,
        @RequestPart("log") testSuitesSourceLog: TestSuitesSourceLog,
    ): Mono<DataBufferFluxResponse> = Mono.fromCallable {
        val dataBufferFlux = testSuitesSourceStorage.download(projectCoordinates, testSuitesSourceLog)
            .map { DefaultDataBufferFactory.sharedInstance.wrap(it) }
            .cast(DataBuffer::class.java)
        ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(dataBufferFlux)
    }
        .onErrorReturn(
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .build()
        )

    /**
     * @param content
     * @param projectCoordinates
     * @param testSuitesSourceLog
     */
    @PostMapping(path = ["/internal/test-suites-source/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart("content") content: Mono<Part>,
        @RequestPart("projectCoordinates", required = false) projectCoordinates: ProjectCoordinates?,
        @RequestPart("log") testSuitesSourceLog: TestSuitesSourceLog,
    ): Mono<EmptyResponse> = content.map { it.content().map(DataBuffer::asByteBuffer) }
        .flatMap { byteBufferFlux ->
            testSuitesSourceStorage.upload(projectCoordinates, testSuitesSourceLog, byteBufferFlux)
                .map {
                    ResponseEntity.status(
                        if (it > 0) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR
                    ).build<Void>()
                }
        }.onErrorReturn(ResponseEntity.status(HttpStatus.CONFLICT).build())
}
