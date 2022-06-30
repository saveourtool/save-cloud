package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.benchmarks.BenchmarkEntity
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.service.GithubLocationPreprocessorService
import com.saveourtool.save.preprocessor.utils.detectLatestSha1
import com.saveourtool.save.preprocessor.utils.generateDirectory
import com.saveourtool.save.testsuite.GitLocation

import com.akuleshov7.ktoml.file.TomlFileReader
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

import java.io.File
import java.nio.file.Path

import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.div
import kotlinx.serialization.serializer

/**
 * A Spring controller for git project downloading
 *
 * @property configProperties config properties
 */
@OptIn(ExperimentalPathApi::class)
@RestController
class AwesomeBenchmarksDownloadController(
    private val configProperties: ConfigProperties,
    private val githubLocationPreprocessorService: GithubLocationPreprocessorService,
) {
    private val webClientBackend = WebClient.create(configProperties.backend)
    private val tmpDir = generateDirectory(File("${configProperties.repository}/awesome-benchmarks"))

    /**
     * Controller to download standard test suites
     *
     * @return Empty response entity
     */
    @Suppress("TOO_LONG_FUNCTION", "TYPE_ALIAS")
    @GetMapping("/download/awesome-benchmarks")
    fun downloadAwesomeBenchmarks() =
            Mono.just(ResponseEntity("Downloading awesome-benchmarks", HttpStatus.ACCEPTED))
                .flatMap { response ->
                    log.debug("Starting to download awesome-benchmarks")
                    githubLocationPreprocessorService.processDirectoryAsMono(gitLocation, gitLocation.detectLatestSha1()) {
                        processDirectoryAndCleanUp(it)
                    }.map { response }
                }
                .doOnEach {
                    log.info("Awesome-benchmarks were downloaded to ${tmpDir.absolutePath}")
                }
                .onErrorResume { exception ->
                    when (exception) {
                        is InvalidRemoteException,
                        is TransportException,
                        is GitAPIException -> log.warn("Error with git API while cloning ${gitLocation.httpUrl} repository", exception)
                        else -> log.warn("Cloning ${gitLocation.httpUrl} repository failed", exception)
                    }
                    Mono.just(ResponseEntity("Downloading of awesome-benchmarks failed", HttpStatus.INTERNAL_SERVER_ERROR))
                }

    private fun processDirectoryAndCleanUp(directory: Path): Mono<Void> {
        val resultList =
                (directory / "benchmarks")
                    .toFile()
                    .walk()
                    .filter { it.isFile }
                    .map { TomlFileReader.decodeFromFile<BenchmarkEntity>(serializer(), it.absolutePath) }

        log.info("Detected and decoded ${resultList.count()} benchmarks from awesome list")

        return webClientBackend
            .post()
            .uri("/upload/awesome-benchmarks")
            .body(BodyInserters.fromValue(resultList.toList()))
            .retrieve()
            .onStatus({status -> status != HttpStatus.OK }) { clientResponse ->
                log.error("Error when making request to /internal/upload/awesome-benchmarks: ${clientResponse.statusCode()}")
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Not able to post awesome-benchmarks to backend due to an internal error"
                )
            }
            .toBodilessEntity()
            .then()
    }

    companion object {
        private const val AWESOME_BENCHMARKS_URL = "https://github.com/saveourtool/awesome-benchmarks.git"

        @JvmStatic
        internal val log = LoggerFactory.getLogger(AwesomeBenchmarksDownloadController::class.java)

        @JvmStatic
        internal val gitLocation = GitLocation(
            httpUrl = AWESOME_BENCHMARKS_URL,
            username = null,
            token = null,
            branch = "master",
            subDirectory = "."
        )
    }
}
