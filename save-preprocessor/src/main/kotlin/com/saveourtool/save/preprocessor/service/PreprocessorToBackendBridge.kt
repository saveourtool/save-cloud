package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.entities.*
import com.saveourtool.save.preprocessor.EmptyResponse
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.utils.debug
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

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

    fun saveTestsSuiteSourceSnapshot(
        testSuitesSource: TestSuitesSourceDto,
        version: String,
        creationTime: Instant,
        content: Flux<ByteBuffer>
    ): Mono<Unit> = webClientBackend.post()
            .uri("/test-suites-source/{organizationName}/{testSuitesSourceName}/upload-snapshot?version={version}&creationTime={creationTime}",
                testSuitesSource.organizationName, testSuitesSource.name,
                version, creationTime.toEpochMilli())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .bodyValue(content)
            .retrieve()
            .bodyToMono()

    fun saveTestSuites(testSuiteDtos: List<TestSuiteDto>): Mono<List<TestSuite>> = webClientBackend.post()
        .uri("/test-suites/save")
        .bodyValue(testSuiteDtos)
        .retrieve()
        .bodyToMono()

    fun saveTests(tests: Flux<TestDto>): Flux<EmptyResponse> = tests
        .buffer(TESTS_BUFFER_SIZE)
        .doOnNext {
            log.debug { "Processing chuck of tests [${it.first()} ... ${it.last()}]" }
        }
        .flatMap { chunk ->
            webClientBackend.post()
                .uri("/tests/save")
                .bodyValue(chunk)
                .retrieve()
                .toBodilessEntity()
        }

    /**
     * @param testSuitesSource
     * @param version
     * @return true if backend knows [version], otherwise -- false
     */
    fun doesTestSuitesSourceContainVersion(testSuitesSource: TestSuitesSourceDto, version: String): Mono<Boolean> =
        webClientBackend.get()
            .uri("/test-suites-source/{organizationName}/{testSuitesSourceName}/contains?version={version}",
                testSuitesSource.organizationName, testSuitesSource.name, version)
            .retrieve()
            .bodyToMono()

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @param version
     * @return list of [TestSuite]
     */
    fun getTestSuites(organizationName: String, testSuitesSourceName: String, version: String) = webClientBackend.get()
        .uri(
            "/test-suites-source/{organizationName}/{testSuitesSourceName}/{version}/get-test-suites",
            organizationName, testSuitesSourceName, version
        )
        .retrieve()
        .bodyToMono<List<TestSuite>>()

    /**
     * @param organizationName
     * @param gitUrl
     * @param testRootPath
     * @param branch
     * @return created of existed [TestSuitesSource]
     */
    fun getTestSuitesSource(
        organizationName: String,
        gitUrl: String,
        testRootPath: String,
        branch: String
    ): Mono<TestSuitesSource> = webClientBackend.post()
        .uri(
            "/test-suites-source/{organizationName}/get-or-create?gitUrl={gitUrl}&testRootPath={testRootPath}&branch={branch}",
            organizationName,
            gitUrl,
            testRootPath,
            branch
        )
        .retrieve()
        .bodyToMono()

    fun getTestSuitesLatestVersion(
        organizationName: String,
        name: String,
    ): Mono<String> = webClientBackend.post()
        .uri(
            "/test-suites-source/{organizationName}/{name}/latest",
            organizationName,
            name,
        )
        .retrieve()
        .bodyToMono()

    fun getOrganization(
        organizationName: String,
    ): Mono<Organization> = webClientBackend.get()
        .uri(
            "/organization/{organizationName}",
            organizationName
        )
        .retrieve()
        .bodyToMono()

    /**
     * @param executionId
     * @return [Execution] found by provided values
     */
    fun getExecution(executionId: Long): Mono<Execution> = webClientBackend.get()
        .uri("/execution?id={id}", executionId)
        .retrieve()
        .bodyToMono()

    companion object {
        private val log = LoggerFactory.getLogger(PreprocessorToBackendBridge::class.java)

        // default Webflux in-memory buffer is 256 KiB
        private const val TESTS_BUFFER_SIZE = 128
    }
}
