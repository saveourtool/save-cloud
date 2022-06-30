package com.saveourtool.save.preprocessor.service

import com.saveourtool.save.entities.TestSuitesSourceLog
import com.saveourtool.save.preprocessor.config.ConfigProperties
import com.saveourtool.save.testsuite.GitLocation
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import org.eclipse.jgit.api.ArchiveCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.archive.TarFormat
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

typealias DirectoryProcessor = (Path) -> Unit
typealias ArchiveWriter = ((OutputStream) -> Unit) -> Unit

/**
 * Additional service for github location
 */
@Service
class GithubLocationPreprocessorService(
    private val configProperties: ConfigProperties,
) {
    private fun createTempDirectory() = Files.createTempDirectory(
        Paths.get(configProperties.repository), GithubLocationPreprocessorService::class.simpleName
    )

    /**
     * @param gitLocation
     * @param sha1
     * @param repositoryProcessor
     * @return result of [repositoryProcessor]
     * @throws GitAPIException
     */
    fun <T> processDirectoryAsMono(
        gitLocation: GitLocation,
        sha1: String,
        repositoryProcessor: (Path) -> Mono<T>,
    ): Mono<T> {
        val tmpDir = createTempDirectory()
        try {
            Git.cloneRepository()
                .setCredentialsProvider(gitLocation.credentialsProvider())
                .setURI(gitLocation.httpUrl)
                .setDirectory(tmpDir.toFile())
                .setRemote(Constants.DEFAULT_REMOTE_NAME)
                .setBranch(sha1)
                .setCloneAllBranches(false)
                .call()
                .use {
                    // need to close Git after all
                }
        } catch (ex: GitAPIException) {
            // clean up in case of exception
            tmpDir.deleteIfExists()
            throw ex
        }
        return Mono.using({ tmpDir },
            { repositoryProcessor(it) },
            { it.deleteIfExists() }
        )
    }

    /**
     * @param testSuitesSourceLog
     * @param repositoryProcessor
     */
    fun <T> processDirectoryAsMono(
        testSuitesSourceLog: TestSuitesSourceLog,
        repositoryProcessor: (Path) -> Mono<T>,
    ): Mono<T> {
        val githubLocation = GitLocation.parseFromDatabase(testSuitesSourceLog.source.locationInfo)
        val sha1 = testSuitesSourceLog.version
        return processDirectoryAsMono(githubLocation, sha1, repositoryProcessor)
    }

    /**
     * @param testSuitesSourceLog
     * @param repositoryProcessor
     */
    fun processDirectory(
        testSuitesSourceLog: TestSuitesSourceLog,
        repositoryProcessor: DirectoryProcessor,
    ) {
        processDirectory(GitLocation.parseFromDatabase(testSuitesSourceLog.source.locationInfo), testSuitesSourceLog.version, repositoryProcessor)
    }

    /**
     * @param gitLocation
     * @param sha1
     * @param repositoryProcessor
     */
    fun processDirectory(
        gitLocation: GitLocation,
        sha1: String,
        repositoryProcessor: DirectoryProcessor,
    ) {
        val tmpDir = createTempDirectory()
        try {
            Git.cloneRepository()
                .setCredentialsProvider(gitLocation.credentialsProvider())
                .setURI(gitLocation.httpUrl)
                .setDirectory(tmpDir.toFile())
                .setRemote(Constants.DEFAULT_REMOTE_NAME)
                .setBranch(sha1)
                .setCloneAllBranches(false)
                .call()
                .use { git ->
                    repositoryProcessor(tmpDir)
                }
        } finally {
            Files.deleteIfExists(tmpDir)
        }
    }

    /**
     * @param gitLocation
     * @param sha1
     * @param repositoryProcessor
     * @param archiveWriter
     */
    fun processDirectoryAndArchive(
        gitLocation: GitLocation,
        sha1: String,
        repositoryProcessor: DirectoryProcessor,
        archiveWriter: ArchiveWriter,
    ) {
        val repositoryProcessorWithArchive: DirectoryProcessor = repositoryProcessor.withArchiveToTar(archiveWriter)
        processDirectory(gitLocation, sha1, repositoryProcessorWithArchive)
    }

    /**
     * @param repositoryPath
     * @param archiveProcess
     * @return result of [archiveProcess]
     */
    fun <T> processTarArchiveAsMono(
        repositoryPath: Path,
        archiveProcess: (InputStream) -> Mono<T>,
    ): Mono<T> = Mono.fromCallable {
        Files.createTempFile(repositoryPath, "archive", "tar")
    }.doOnSuccess { archive ->
        archive.outputStream().use {
            archiveToTar(repositoryPath, it)
        }
    }.flatMap { archiveFile ->
        archiveProcess(archiveFile.inputStream())
            .doOnNext { archiveFile.deleteExisting() }
    }

    private fun DirectoryProcessor.withArchiveToTar(archiveWriter: ArchiveWriter): DirectoryProcessor = { path: Path ->
        // call original logic
        this(path)
        // then archiving
        archiveWriter.invoke { out -> archiveToTar(path, out) }
    }

    @SuppressWarnings("MaxLineLength")
    /**
     * [How to use JGit to push changes to remote with OAuth access token](https://stackoverflow.com/questions/28073266/how-to-use-jgit-to-push-changes-to-remote-with-oauth-access-token)
     *
     * @return [CredentialsProvider]
     */
    private fun GitLocation.credentialsProvider(): CredentialsProvider =
            token?.let { UsernamePasswordCredentialsProvider(username, token) } ?: CredentialsProvider.getDefault()

    companion object {
        private val log = LoggerFactory.getLogger(GithubLocationPreprocessorService::class.java)
        private const val TAR_FORMAT_NAME = "tar"

        /**
         * @param repositoryPath
         * @param outputStream
         */
        fun archiveToTar(
            repositoryPath: Path,
            outputStream: OutputStream,
        ) {
            ArchiveCommand.registerFormat(TAR_FORMAT_NAME, TarFormat())
            try {
                RepositoryBuilder()
                    .setWorkTree(repositoryPath.toFile())
                    .setMustExist(true)
                    .build()
                    .use { repository ->
                        Git(repository).use { git ->
                            git.archive()
                                .setFormat(TAR_FORMAT_NAME)
                                .setTree(repository.resolve(Constants.HEAD))
                                .setOutputStream(outputStream)
                                .call()
                        }
                    }
            } finally {
                ArchiveCommand.unregisterFormat(TAR_FORMAT_NAME)
            }
        }

        /**
         * @param repositoryPath
         * @param inputStream
         */
        @Suppress("NestedBlockDepth")
        fun extractFromTar(
            repositoryPath: Path,
            inputStream: InputStream
        ) {
            inputStream.use { buffIn ->
                TarArchiveInputStream(buffIn).use { tarIn ->
                    generateSequence { tarIn.nextTarEntry }.forEach { tarArchiveEntry ->
                        val extractedPath = repositoryPath.resolve(tarArchiveEntry.name)
                        if (tarArchiveEntry.isDirectory) {
                            Files.createDirectories(extractedPath)
                        } else {
                            extractedPath.outputStream().buffered().use {
                                IOUtils.copy(tarIn, it)
                            }
                        }
                    }
                }
            }
        }
    }
}
