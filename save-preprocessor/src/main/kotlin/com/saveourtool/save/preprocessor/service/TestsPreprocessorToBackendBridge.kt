package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.entities.*
import com.saveourtool.save.preprocessor.EmptyResponse
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.debug
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * A bridge from preprocesor to backend (rest api wrapper)
 */
@Service
class TestsPreprocessorToBackendBridge(
    configProperties: ConfigProperties,
    kotlinSerializationWebClientCustomizer: WebClientCustomizer,
) {
    private val webClientBackend = WebClient.builder()
        .baseUrl(configProperties.backend)
        .apply(kotlinSerializationWebClientCustomizer::customize)
        .build()

    /**
     * @param testSuitesSource
     * @param version
     * @param creationTime
     * @param resourceWithContent
     * @return empty response
     */
    fun saveTestsSuiteSourceSnapshot(
        testSuitesSource: TestSuitesSourceDto,
        version: String,
        creationTime: Instant,
        resourceWithContent: Resource,
    ): Mono<Unit> = webClientBackend.post()
        .uri("/test-suites-sources/{organizationName}/{testSuitesSourceName}/upload-snapshot?version={version}&creationTime={creationTime}",
            testSuitesSource.organizationName, testSuitesSource.name,
            version, creationTime.toEpochMilli())
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData("content", resourceWithContent))
        .retrieve()
        .bodyToMono()

    /**
     * @param testSuitesSource
     * @param version
     * @return true if backend knows [version], otherwise -- false
     */
    fun doesTestSuitesSourceContainVersion(testSuitesSource: TestSuitesSourceDto, version: String): Mono<Boolean> =
            webClientBackend.get()
                .uri("/test-suites-sources/{organizationName}/{testSuitesSourceName}/contains-snapshot?version={version}",
                    testSuitesSource.organizationName, testSuitesSource.name, version)
                .retrieve()
                .bodyToMono()

    /**
     * @param testSuitesSource
     * @param version
     * @return result of delete operation
     */
    fun deleteTestSuitesSourceVersion(testSuitesSource: TestSuitesSourceDto, version: String): Mono<Unit> =
            webClientBackend.delete()
                .uri("/test-suites-sources/{organizationName}/{testSuitesSourceName}/delete-snapshot?version={version}",
                    testSuitesSource.organizationName, testSuitesSource.name, version)
                .retrieve()
                .bodyToMono<Boolean>()
                .map { isDeleted ->
                    with(testSuitesSource) {
                        log.debug {
                            "Result of delete operation for $name in $organizationName is $isDeleted"
                        }
                    }
                }

    /**
     * @param testSuiteDtos
     * @return list of saved [TestSuite]
     */
    fun saveTestSuites(testSuiteDtos: List<TestSuiteDto>): Mono<List<TestSuite>> = webClientBackend.post()
        .uri("/saveTestSuites")
        .bodyValue(testSuiteDtos)
        .retrieve()
        .bodyToMono()

    /**
     * @param testSuitesSource
     * @param version
     * @return empty response
     */
    fun deleteTestSuites(testSuitesSource: TestSuitesSourceDto, version: String): Mono<Unit> = webClientBackend.delete()
        .uri(
            "/test-suites-sources/{organizationName}/{testSuitesSourceName}/delete-test-suites?version={version}",
            testSuitesSource.organizationName, testSuitesSource.name, version
        )
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
