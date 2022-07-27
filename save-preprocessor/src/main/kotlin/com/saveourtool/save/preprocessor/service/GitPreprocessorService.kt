package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.utils.cloneToDirectory
import com.saveourtool.save.utils.*
import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import kotlin.io.path.deleteExisting

typealias GitRepositoryProcessor<T> = (Path, Instant) -> Mono<T>

/**
 * Additional service for Git based [com.saveourtool.save.entities.TestSuitesSource]s
 */
@Service
class GitPreprocessorService(
    private val configProperties: ConfigProperties,
) {
    private fun createTempDirectoryForRepository() = Files.createTempDirectory(
        Paths.get(configProperties.repository), GitPreprocessorService::class.simpleName
    )

    private fun createTempTarFile() = Files.createTempFile(
        Paths.get(configProperties.repository),
        GitPreprocessorService::class.simpleName,
        TAR_EXTENSION
    )

    /**
     * @param gitDto
     * @param branch
     * @param sha1
     * @param repositoryProcessor
     * @return result of [repositoryProcessor]
     * @throws IllegalStateException
     */
    fun <T> cloneAndProcessDirectory(
        gitDto: GitDto,
        branch: String,
        sha1: String,
        repositoryProcessor: GitRepositoryProcessor<T>,
    ): Mono<T> {
        val cloneAction: () -> Pair<Path, Instant> = {
            val tmpDir = createTempDirectoryForRepository()
            val creationTime = try {
                gitDto.cloneToDirectory(branch, sha1, tmpDir)
            } catch (ex: IllegalStateException) {
                log.error(ex) { "Failed to clone git repository ${gitDto.url}" }
                // clean up will be handled by Mono.onComplete and Mono.doOnError
//                FileSystemUtils.deleteRecursively(tmpDir)
                throw ex
            }
            tmpDir to creationTime
        }
        return Mono.fromSupplier(cloneAction)
            .flatMap { (directory, creationTime) -> repositoryProcessor(directory, creationTime)
                .doAfterTerminate { directory.deleteRecursivelySafely() }
            }
    }

    /**
     * @param pathToRepository
     * @return archived git repository, file will be deleted after release Flux
     * @throws IOException
     */
    fun archiveToTar(
        pathToRepository: Path
    ): Flux<ByteBuffer> {
        val archiveAction: () -> Path = {
            val tmpFile = createTempTarFile()
            try {
                pathToRepository.compressAsTarTo(tmpFile)
            } catch (ex: IOException) {
                log.error(ex) { "Failed to archive git repository $pathToRepository" }
                throw ex
            }
            tmpFile
        }
        return Mono.fromSupplier(archiveAction)
            .flatMapMany { tmpFile ->
                tmpFile.toByteBufferFlux().doAfterTerminate {
                    tmpFile.deleteSafely()
                }
            }
    }

    private fun Path.deleteRecursivelySafely() {
        try {
            FileSystemUtils.deleteRecursively(this)
        } catch (ex: Exception) {
            log.error(ex) { "Skip error during clean-up folder" }
        }
    }

    private fun Path.deleteSafely() {
        try {
            Files.deleteIfExists(this)
        } catch (ex: Exception) {
            log.debug(ex) { "Skip error during clean-up file" }
        }
    }

    companion object {
        private val log: Logger = getLogger<GitPreprocessorService>()
    }
}
