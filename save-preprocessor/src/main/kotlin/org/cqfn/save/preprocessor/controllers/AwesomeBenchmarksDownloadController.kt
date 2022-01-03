package org.cqfn.save.preprocessor.controllers

import kotlinx.serialization.serializer
import org.cqfn.save.entities.GitDto
import org.cqfn.save.preprocessor.TextResponse
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.utils.cloneFromGit
import org.cqfn.save.preprocessor.utils.generateDirectory
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.eclipse.jgit.api.errors.TransportException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import com.akuleshov7.ktoml.file.TomlFileReader
import kotlinx.serialization.serializer
import kotlinx.serialization.Serializable
import org.cqfn.save.awesome.benchmarks.BenchmarkInfo
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.div

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
    /**
     * Controller to download standard test suites
     *
     * @return Empty response entity
     */
    @Suppress("TOO_LONG_FUNCTION", "TYPE_ALIAS")
    @GetMapping("/upload/awesome/benchmarks")
    fun downloadAwesomeBenchmarks(): Mono<TextResponse> =
            Mono.just(ResponseEntity("Downloading awesome-benchmarks", HttpStatus.ACCEPTED))
                    .doOnSuccess {
                        log.info("Starting to download awesome-benchmarks to ${tmpDir.absolutePath}")
                        cloneFromGit(gitDto, tmpDir)
                        log.info("Awesome-benchmarks were downloaded to ${tmpDir.absolutePath}")
                        processDirectoryAndCleanUp()
                    }.onErrorResume { exception ->
                        tmpDir.deleteRecursively()
                        when (exception) {
                            is InvalidRemoteException,
                            is TransportException,
                            is GitAPIException -> log.warn("Error with git API while cloning ${gitDto.url} repository", exception)
                            else -> log.warn("Cloning ${gitDto.url} repository failed", exception)
                        }
                        Mono.just(ResponseEntity("Downloading of awesome-benchmarks failed", HttpStatus.INTERNAL_SERVER_ERROR))
                    }

    private fun processDirectoryAndCleanUp() {
        (tmpDir.toPath() / "benchmarks").toFile().walk().forEach {
            if (it.isFile) {
                val resultFromString = TomlFileReader.decodeFromFile<BenchmarkInfo>(serializer(), it.absolutePath)
                println(resultFromString)
            }
        }

        tmpDir.deleteRecursively()
    }

    companion object {
        private const val AWESOME_BENCHMARKS_URL = "https://github.com/analysis-dev/awesome-benchmarks.git"

        @JvmStatic
        internal val tmpDir = generateDirectory(listOf(AWESOME_BENCHMARKS_URL), "awesome-benchmarks")

        @JvmStatic
        internal val log = LoggerFactory.getLogger(AwesomeBenchmarksDownloadController::class.java)

        @JvmStatic
        internal val gitDto = GitDto(AWESOME_BENCHMARKS_URL)
    }
}

