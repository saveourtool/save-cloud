package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestSuitesSourceLog
import com.saveourtool.save.preprocessor.EmptyResponse
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.io.InputStream

/**
 * A bridge from preprocesor to backend (rest api wrapper)
 */
@Service
class PreprocessorToBackendBridge(
    configProperties: ConfigProperties,
    kotlinSerializationWebClientCustomizer: WebClientCustomizer,
) {
    private val webClientBackend = WebClient.builder()
        .baseUrl(configProperties.backend)
        .apply(kotlinSerializationWebClientCustomizer::customize)
        .build()

    /**
     * @param testSuitesSourceDto DTO of [TestSuitesSourceDto]
     * @param version version to be found
     * @return found instance [TestSuitesSourceLog]
     */
    @PostMapping("/internal/test-suites-source/find-version")
    fun findTestSuiteSourceLog(
        @RequestBody testSuitesSourceDto: TestSuitesSourceDto,
        @RequestParam version: String
    ): Mono<TestSuitesSourceLog> = webClientBackend.post()
        .uri("/test-suites-source/find-version?version={version}")
        .bodyValue(testSuitesSourceDto)
        .retrieve()
        .bodyToMono()

    /**
     * @param testSuitesSourceDto
     * @param version
     * @return if there is a log with provided version -- true, otherwise false
     */
    fun checkVersionForTestSuitesSource(testSuitesSourceDto: TestSuitesSourceDto, version: String): Mono<Boolean> = webClientBackend.post()
        .uri("/test-suites-source/check-version?version={version}", version)
        .bodyValue(testSuitesSourceDto)
        .retrieve()
        .bodyToMono()

    /**
     * @param testSuitesSourceDto
     * @param version
     * @return a new instance of [TestSuitesSourceLog]
     */
    fun createNewTestSuiteSourceLog(testSuitesSourceDto: TestSuitesSourceDto, version: String): Mono<TestSuitesSourceLog> = webClientBackend.post()
        .uri("/test-suites-source/log-new-version?version={version}", version)
        .bodyValue(testSuitesSourceDto)
        .retrieve()
        .bodyToMono()

    /**
     * @param name
     * @return entity of [TestSuitesSource] or null
     */
    fun findTestSuitesSourceByName(name: String): Mono<TestSuitesSource> = webClientBackend.get()
        .uri("/test-suites-source/find-by-name?name={name}", name)
        .retrieve()
        .bodyToMono()

    /**
     * @param dto
     * @return saved entity as [TestSuitesSource]
     */
    fun createTestSuitesSource(dto: TestSuitesSourceDto): Mono<TestSuitesSource> = webClientBackend.post()
        .uri("/test-suites-source/register-new")
        .bodyValue(dto)
        .retrieve()
        .bodyToMono()

    /**
     * @param dto
     * @return found and new entity as [TestSuitesSource]
     */
    fun findOrCreateTestSuitesSource(dto: TestSuitesSourceDto): Mono<TestSuitesSource> =
            findTestSuitesSourceByName(dto.name)
                .switchIfEmpty { createTestSuitesSource(dto) }

    /**
     * @param testSuiteIds
     * @return a single instance of [TestSuitesSource]
     */
    fun getSingleTestSuitesSourceByTestSuiteIds(testSuiteIds: List<Long>): Mono<TestSuitesSource> = webClientBackend.post()
        .uri("/test-suites-source/get-single-by-test-suites-ids")
        .bodyValue(testSuiteIds)
        .retrieve()
        .bodyToMono()

    /**
     * @param inputStreamAsMono
     * @param projectCoordinates
     * @param testSuitesSourceLog
     * @return empty response
     */
    fun uploadTestSuitesSourceContent(
        inputStreamAsMono: Mono<InputStream>,
        projectCoordinates: ProjectCoordinates?,
        testSuitesSourceLog: TestSuitesSourceLog,
    ): Mono<EmptyResponse> = inputStreamAsMono.flatMap {
        uploadTestSuitesSourceContent(it, projectCoordinates, testSuitesSourceLog)
    }

    /**
     * @param inputStream
     * @param projectCoordinates
     * @param testSuitesSourceLog
     * @return empty response
     */
    fun uploadTestSuitesSourceContent(
        inputStream: InputStream,
        projectCoordinates: ProjectCoordinates?,
        testSuitesSourceLog: TestSuitesSourceLog,
    ): Mono<EmptyResponse> = webClientBackend.post()
        .uri("/test-suites-source/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData("log", testSuitesSourceLog)
            .also { builder -> projectCoordinates?.also { builder.with("projectCoordinates", it) } }
            .with("content", InputStreamResource(inputStream))
            .with("log", testSuitesSourceLog))
        .retrieve()
        .toBodilessEntity()

    /**
     * @param testSuitesSourceLog
     * @return list of [TestSuite] related to provided [TestSuitesSourceLog]
     */
    fun findAllTestSuitesByLog(
        testSuitesSourceLog: TestSuitesSourceLog,
    ): Mono<List<TestSuite>> = webClientBackend.post()
        .uri("/test-suites/find-by-log")
        .bodyValue(testSuitesSourceLog)
        .retrieve()
        .bodyToMono()
}
