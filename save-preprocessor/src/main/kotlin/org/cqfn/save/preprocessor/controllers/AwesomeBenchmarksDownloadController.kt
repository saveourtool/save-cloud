package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.benchmarks.BenchmarkEntity
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.utils.generateDirectory
import org.cqfn.save.preprocessor.utils.pullOrCloneFromGit

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
) {
    private val webClientBackend = WebClient.create(configProperties.backend)
    private val tmpDir = generateDirectory(listOf(AWESOME_BENCHMARKS_URL), "${configProperties.repository}/awesome-benchmarks")

    /**
     * Controller to download standard test suites
     *
     * @return Empty response entity
     */
    @Suppress("TOO_LONG_FUNCTION", "TYPE_ALIAS")
    @GetMapping("/download/awesome-benchmarks")
    fun downloadAwesomeBenchmarks() =
            Mono.just(ResponseEntity("Downloading awesome-benchmarks", HttpStatus.ACCEPTED))
                .doOnSuccess {
                    log.debug("Starting to download awesome-benchmarks to ${tmpDir.absolutePath}")
                    pullOrCloneFromGit(gitDto, tmpDir)
                    log.info("Awesome-benchmarks were downloaded to ${tmpDir.absolutePath}")
                    processDirectoryAndCleanUp().subscribe()
                    tmpDir.deleteRecursively()
                }
                .onErrorResume { exception ->
                    tmpDir.deleteRecursively()
                    when (exception) {
                        is InvalidRemoteException,
                        is TransportException,
                        is GitAPIException -> log.warn("Error with git API while cloning ${gitDto.url} repository", exception)
                        else -> log.warn("Cloning ${gitDto.url} repository failed", exception)
                    }
                    Mono.just(ResponseEntity("Downloading of awesome-benchmarks failed", HttpStatus.INTERNAL_SERVER_ERROR))
                }

    private fun processDirectoryAndCleanUp(): Mono<Void> {
        val resultList =
                (tmpDir.toPath() / "benchmarks")
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
        private const val AWESOME_BENCHMARKS_URL = "https://github.com/analysis-dev/awesome-benchmarks.git"

        @JvmStatic
        internal val log = LoggerFactory.getLogger(AwesomeBenchmarksDownloadController::class.java)

        @JvmStatic
        internal val gitDto = GitDto(AWESOME_BENCHMARKS_URL)
    }
}
