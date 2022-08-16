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
     * @return true if backend knows [version], otherwise -- false
     */
    fun removeTestSuitesSourceWithVersion(testSuitesSource: TestSuitesSourceDto, version: String): Mono<Boolean> =
        webClientBackend.get()
            .uri("/test-suites-sources/{organizationName}/{testSuitesSourceName}/remove-snapshot?version={version}",
                testSuitesSource.organizationName, testSuitesSource.name, version)
            .retrieve()
            .bodyToMono()


    /**
     * @param testSuitesSource
     * @return list of [TestSuitesSourceSnapshotKey] related to [testSuitesSource]
     */
    fun listTestSuitesSourceVersions(testSuitesSource: TestSuitesSourceDto): Mono<TestSuitesSourceSnapshotKeyList> =
            webClientBackend.get()
                .uri("/test-suites-sources/{organizationName}/{testSuitesSourceName}/list-snapshot",
                    testSuitesSource.organizationName, testSuitesSource.name)
                .retrieve()
                .bodyToMono()

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @param version
     * @return list of [TestSuite]
     */
    fun getTestSuites(
        organizationName: String,
        testSuitesSourceName: String,
        version: String
    ) = webClientBackend.get()
        .uri(
            "/test-suites-sources/{organizationName}/{testSuitesSourceName}/get-test-suites?version={version}",
            organizationName, testSuitesSourceName, version
        )
        .retrieve()
        .bodyToMono<List<TestSuite>>()

    /**
     * @param organizationName
     * @param gitUrl
     * @param testRootPath
     * @param branch
     * @return created of existed [TestSuitesSourceDto]
     */
    fun getOrCreateTestSuitesSource(
        organizationName: String,
        gitUrl: String,
        testRootPath: String,
        branch: String
    ): Mono<TestSuitesSourceDto> = webClientBackend.post()
        .uri(
            "/test-suites-sources/{organizationName}/get-or-create?gitUrl={gitUrl}&testRootPath={testRootPath}&branch={branch}",
            organizationName,
            gitUrl,
            testRootPath,
            branch
        )
        .retrieve()
        .bodyToMono()

    /**
     * Will be removed in phase 3
     *
     * @return list of standard test suites sourcers
     */
    fun getStandardTestSuitesSources(): Mono<TestSuitesSourceDtoList> = webClientBackend.get()
        .uri("/test-suites-sources/get-standard")
        .retrieve()
        .bodyToMono()

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
