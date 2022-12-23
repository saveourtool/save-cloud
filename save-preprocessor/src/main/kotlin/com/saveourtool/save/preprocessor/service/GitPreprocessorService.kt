package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.preprocessor.utils.cloneBranchToDirectory
import com.saveourtool.save.preprocessor.utils.cloneCommitToDirectory
import com.saveourtool.save.preprocessor.utils.cloneTagToDirectory
import com.saveourtool.save.utils.*
import org.eclipse.jgit.util.FileUtils
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

typealias GitRepositoryProcessor<T> = (Path, Instant) -> Mono<T>
typealias ArchiveProcessor<T> = (Path) -> Mono<T>

/**
 * Additional service for Git based [com.saveourtool.save.entities.TestSuitesSource]s
 */
@Service
class GitPreprocessorService(
    configProperties: ConfigProperties,
) {
    private val workingDir = Paths.get(configProperties.repository, "tmp")
        .also { it.createDirectories() }

    private fun createTempDirectoryForRepository() = Files.createTempDirectory(
        workingDir,
        "repository-"
    )

    private fun createTempTarFile() = Files.createTempFile(
        workingDir,
        "archive-",
        ARCHIVE_EXTENSION
    )

    /**
     * @param gitDto
     * @param tagName
     * @param repositoryProcessor operation on folder should be finished here -- folder will be removed after it
     * @return result of [repositoryProcessor]
     * @throws IllegalStateException
     * @throws Exception
     */
    fun <T> cloneTagAndProcessDirectory(
        gitDto: GitDto,
        tagName: String,
        repositoryProcessor: GitRepositoryProcessor<T>,
    ): Mono<T> = doCloneAndProcessDirectory(gitDto, repositoryProcessor) {
        cloneTagToDirectory(tagName, it)
    }

    /**
     * @param gitDto
     * @param branchName
     * @param repositoryProcessor operation on folder should be finished here -- folder will be removed after it
     * @return result of [repositoryProcessor]
     * @throws IllegalStateException
     * @throws Exception
     */
    fun <T> cloneBranchAndProcessDirectory(
        gitDto: GitDto,
        branchName: String,
        repositoryProcessor: GitRepositoryProcessor<T>,
    ): Mono<T> = doCloneAndProcessDirectory(gitDto, repositoryProcessor) {
        cloneBranchToDirectory(branchName, it)
    }

    /**
     * @param gitDto
     * @param commitId
     * @param repositoryProcessor operation on folder should be finished here -- folder will be removed after it
     * @return result of [repositoryProcessor]
     * @throws IllegalStateException
     * @throws Exception
     */
    fun <T> cloneCommitAndProcessDirectory(
        gitDto: GitDto,
        commitId: String,
        repositoryProcessor: GitRepositoryProcessor<T>,
    ): Mono<T> = doCloneAndProcessDirectory(gitDto, repositoryProcessor) {
        cloneCommitToDirectory(commitId, it)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <T> doCloneAndProcessDirectory(
        gitDto: GitDto,
        repositoryProcessor: GitRepositoryProcessor<T>,
        doCloneToDirectory: GitDto.(Path) -> Instant,
    ): Mono<T> {
        val cloneAction: () -> Pair<Path, Instant> = {
            val tmpDir = createTempDirectoryForRepository()
            val creationTime = try {
                gitDto.doCloneToDirectory(tmpDir)
            } catch (ex: Exception) {
                log.error(ex) { "Failed to clone git repository ${gitDto.url}" }  //------------------------------------
                tmpDir.deleteRecursivelySafely()
                throw ex
            }
            tmpDir to creationTime
        }
        return Mono.usingWhen(
            Mono.fromSupplier(cloneAction),
            { (directory, creationTime) -> repositoryProcessor(directory, creationTime) },
            { (directory, _) -> directory.deleteRecursivelySafelyAsync() }
        )
    }

    /**
     * @param pathToRepository
     * @param archiveProcessor operation on file should be finished here -- file will be removed after it
     * @return archived git repository, file will be deleted after release Flux
     * @throws IOException
     * @throws Exception
     */
    @Suppress("TooGenericExceptionCaught")
    fun <T> archiveToTar(
        pathToRepository: Path,
        archiveProcessor: ArchiveProcessor<T>
    ): Mono<T> {
        val archiveAction: () -> Path = {
            val tmpFile = createTempTarFile()
            try {
                pathToRepository.compressAsZipTo(tmpFile)
            } catch (ex: Exception) {
                log.error(ex) { "Failed to archive git repository $pathToRepository" }
                tmpFile.deleteRecursivelySafely()
                throw ex
            }
            tmpFile
        }
        return Mono.usingWhen(
            Mono.fromSupplier(archiveAction),
            { tmpFile -> archiveProcessor(tmpFile) },
            { tmpFile -> tmpFile.deleteRecursivelySafelyAsync() }
        )
    }

    private fun Path.deleteRecursivelySafelyAsync() = Mono.fromCallable { deleteRecursivelySafely() }
        .subscribeOn(Schedulers.boundedElastic())

    @Suppress("TooGenericExceptionCaught")
    private fun Path.deleteRecursivelySafely() {
        try {
            log.info {
                "Start cleanup of ${absolutePathString()}"
            }
            FileUtils.delete(toFile(), FileUtils.RECURSIVE or FileUtils.IGNORE_ERRORS or FileUtils.RETRY)
        } catch (ex: Exception) {
            log.error(ex) { "Skip error during clean-up folder" }
        }
    }

    companion object {
        private val log: Logger = getLogger<GitPreprocessorService>()
    }
}
