package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestsSourceSnapshotRepository
import com.saveourtool.save.backend.repository.TestsSourceVersionRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.storage.MigrationTestsSourceSnapshotStorage
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceVersion
import com.saveourtool.save.entities.TestsSourceVersion.Companion.toEntity
import com.saveourtool.save.test.*
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.utils.*

import org.springframework.context.annotation.Lazy
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer

import kotlin.io.path.*

/**
 * Service for [TestsSourceVersionInfo]
 */
@Service
class TestsSourceVersionService(
    @Lazy
    private val snapshotStorage: MigrationTestsSourceSnapshotStorage,
    private val testSuitesService: TestSuitesService,
    private val testsSourceSnapshotRepository: TestsSourceSnapshotRepository,
    private val testsSourceVersionRepository: TestsSourceVersionRepository,
    private val userRepository: UserRepository,
    configProperties: ConfigProperties,
) {
    private val tmpDir = (java.nio.file.Path.of(configProperties.fileStorage.location) / "tmp").createDirectories()

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return true if storage contains snapshot with provided values, otherwise -- false
     */
    fun doesContain(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Mono<Boolean> = snapshotStorage.findSnapshotKey(organizationName, sourceName, version)
        .map { true }
        .defaultIfEmpty(false)

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return content of a key which contains provided values
     */
    fun download(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Flux<ByteBuffer> = snapshotStorage.findSnapshotKey(
        organizationName = organizationName,
        sourceName = sourceName,
        version = version,
    ).flatMapMany { snapshotStorage.download(it) }

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return result of deletion of a key which contains provided values
     */
    fun delete(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Mono<Boolean> = snapshotStorage.findSnapshotKey(
        organizationName = organizationName,
        sourceName = sourceName,
        version = version,
    ).flatMap { snapshotStorage.delete(it) }

    /**
     * @param organizationName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun list(
        organizationName: String,
    ): Flux<TestsSourceVersionInfo> = snapshotStorage.list(organizationName)

    /**
     * @param organizationName
     * @param sourceName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun list(
        organizationName: String,
        sourceName: String,
    ): Flux<TestsSourceVersionInfo> = snapshotStorage.list(organizationName, sourceName)

    /**
     * @param request
     * @return [TestFilesContent] filled with test files
     */
    fun getTestContent(request: TestFilesRequest): Mono<TestFilesContent> = with(request.testSuitesSource) {
        snapshotStorage.findSnapshotKey(organizationName, name, request.version)
            .orNotFound {
                "There is no content for tests from $name in $organizationName with version ${request.version}"
            }
    }
        .flatMap { key ->
            val tmpSourceDir = createTempDirectory(tmpDir, "source-")
            val tmpArchive = createTempFile(tmpSourceDir, "archive-", ARCHIVE_EXTENSION)
            val sourceContent = snapshotStorage.download(key)
                .map { DefaultDataBufferFactory.sharedInstance.wrap(it) }
                .cast(DataBuffer::class.java)

            DataBufferUtils.write(sourceContent, tmpArchive.outputStream())
                .map { DataBufferUtils.release(it) }
                .collectList()
                .map {
                    tmpArchive.extractZipHere()
                    tmpArchive.deleteExisting()
                }
                .map {
                    val testFilePath = request.test.filePath
                    val additionalTestFilePath = request.test.additionalFiles.firstOrNull()
                    val result = TestFilesContent(
                        tmpSourceDir.resolve(testFilePath).readLines(),
                        additionalTestFilePath?.let { tmpSourceDir.resolve(it).readLines() },
                    )
                    tmpSourceDir.toFile().deleteRecursively()
                    result
                }
        }

    /**
     * Saves [TestsSourceVersion] created from provided [TestsSourceVersionDto]
     *
     * @param dto
     */
    @Transactional
    fun save(
        dto: TestsSourceVersionDto,
    ) {
        val entity = dto.toEntity(
            snapshotResolver = ::getSnapshot,
            userResolver = userRepository::getByIdOrNotFound
        )
        val savedEntity = testsSourceVersionRepository.save(entity)
        // copy test suites
        testSuitesService.copyToNewVersion(
            sourceId = savedEntity.snapshot.source.requiredId(),
            originalVersion = savedEntity.snapshot.commitId,
            newVersion = savedEntity.name,
        )
    }

    private fun getSnapshot(dto: TestsSourceSnapshotDto) = testsSourceSnapshotRepository.findBySource_IdAndCommitId(
        sourceId = dto.sourceId,
        commitId = dto.commitId,
    ).orNotFound {
        "Not found ${TestsSourceSnapshot::class.simpleName} for $dto"
    }
}
