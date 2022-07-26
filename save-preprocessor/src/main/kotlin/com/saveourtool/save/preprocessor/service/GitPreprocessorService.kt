package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.utils.cloneToDirectory
import com.saveourtool.save.utils.TAR_EXTENSION
import com.saveourtool.save.utils.compressAsTarTo
import com.saveourtool.save.utils.toByteBufferFlux
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
        repositoryProcessor: (Path, Instant) -> Mono<T>,
    ): Mono<T> {
        val cloneAction: () -> Pair<Path, Instant> = {
            val tmpDir = createTempDirectoryForRepository()
            val creationTime = try {
                gitDto.cloneToDirectory(branch, sha1, tmpDir)
            } catch (ex: IllegalStateException) {
                // clean up in case of exception
                FileSystemUtils.deleteRecursively(tmpDir)
                throw ex
            }
            tmpDir to creationTime
        }
        return Mono.using(
            cloneAction,
            { (directory, creationTime) -> repositoryProcessor(directory, creationTime) },
            { (directory, _) -> FileSystemUtils.deleteRecursively(directory) },
        )
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
                tmpFile.deleteExisting()
                throw ex
            }
            tmpFile
        }
        return Flux.using(
            archiveAction,
            { it.toByteBufferFlux() },
            Files::delete
        )
    }
}
