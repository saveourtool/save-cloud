package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.entities.*
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.utils.GitCommitInfo
import com.saveourtool.save.spring.utils.applyAll
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.EmptyResponse
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getCurrentLocalDateTime

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
     * @param testSuitesSource
     * @param version
     * @param gitCommitInfo
     * @param resourceWithContent
     * @return empty response
     */
    fun saveTestsSuiteSourceSnapshot(
        testSuitesSource: TestSuitesSourceDto,
        version: String,
        gitCommitInfo: GitCommitInfo,
        resourceWithContent: Resource,
    ): Mono<Unit> = webClientBackend.post()
        .uri("/test-suites-sources/upload-snapshot")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(
            BodyInserters.fromMultipartData("content", resourceWithContent)
                .with(
                    "versionInfo",
                    TestsSourceVersionInfo(
                        organizationName = testSuitesSource.organizationName,
                        sourceName = testSuitesSource.name,
                        version = version,
                        creationTime = getCurrentLocalDateTime(),
                        commitId = gitCommitInfo.id,
                        commitTime = gitCommitInfo.time,
                    )
                )
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
