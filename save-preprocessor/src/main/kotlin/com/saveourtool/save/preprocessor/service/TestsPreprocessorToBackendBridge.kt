package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.entities.*
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.spring.utils.applyAll
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.test.TestsSourceVersionDto
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.EmptyResponse
import com.saveourtool.save.utils.debug

import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * A bridge from preprocesor to backend (rest api wrapper)
 */
@Service
class TestsPreprocessorToBackendBridge(
    configProperties: ConfigProperties,
    customizers: List<WebClientCustomizer>,
) {
    private val webClientBackend = WebClient.builder()
        .baseUrl(configProperties.backend)
        .applyAll(customizers)
        .build()

    /**
     * @param snapshotDto
     * @param resourceWithContent
     * @return empty response
     */
    fun saveTestsSuiteSourceSnapshot(
        snapshotDto: TestsSourceSnapshotDto,
        resourceWithContent: Resource,
    ): Mono<Unit> = webClientBackend.post()
        .uri("/test-suites-sources/upload-snapshot")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(
            BodyInserters.fromMultipartData("content", resourceWithContent)
                .with("snapshot", snapshotDto)
        )
        .retrieve()
        .onStatus({ !it.is2xxSuccessful }) {
            Mono.error(
                IllegalStateException("Failed to upload test suite source snapshot",
                    ResponseStatusException(it.statusCode())
                )
            )
        }
        .bodyToMono()

    /**
     * @param testsSourceSnapshotDto
     * @return true if backend knows [testsSourceSnapshotDto], otherwise -- false
     */
    fun doesContainTestsSourceSnapshot(testsSourceSnapshotDto: TestsSourceSnapshotDto): Mono<Boolean> =
            webClientBackend
                .post()
                .uri("/test-suites-sources/contains-snapshot")
                .bodyValue(testsSourceSnapshotDto)
                .retrieve()
                .bodyToMono()

    /**
     * @param testsSourceVersionDto
     * @return empty response
     */
    fun saveTestsSourceVersion(testsSourceVersionDto: TestsSourceVersionDto): Mono<Unit> = webClientBackend
        .post()
        .uri("/test-suites-sources/save-version")
        .bodyValue(testsSourceVersionDto)
        .retrieve()
        .toBodilessEntity()
        .then(Mono.just(Unit))

    /**
     * @param testSuiteDto
     * @return saved [TestSuite]
     */
    fun saveTestSuite(testSuiteDto: TestSuiteDto): Mono<TestSuite> = webClientBackend.post()
        .uri("/test-suites/save")
        .bodyValue(testSuiteDto)
        .retrieve()
        .bodyToMono()

    /**
     * @param tests
     * @return empty response
     */
    fun saveTests(tests: Flux<TestDto>): Flux<EmptyResponse> = tests
        .buffer(TESTS_BUFFER_SIZE)
        .doOnNext {
            log.debug { "Processing chuck of tests [${it.first()} ... ${it.last()}]" }
        }
        .flatMap { chunk ->
            webClientBackend.post()
                .uri("/initializeTests")
                .bodyValue(chunk)
                .retrieve()
                .toBodilessEntity()
        }

    companion object {
        private val log = LoggerFactory.getLogger(TestsPreprocessorToBackendBridge::class.java)

        // default Webflux in-memory buffer is 256 KiB
        private const val TESTS_BUFFER_SIZE = 128
    }
}
