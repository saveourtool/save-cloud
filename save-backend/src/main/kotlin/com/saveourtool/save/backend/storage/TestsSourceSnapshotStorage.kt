package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.TestSuitesService
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceSnapshot.Companion.toEntity
import com.saveourtool.save.request.TestFilesRequest
import com.saveourtool.save.storage.AbstractStorageWithDatabase
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.utils.*

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import java.nio.file.Path

import kotlin.io.path.*

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Component
class TestsSourceSnapshotStorage(
    configProperties: ConfigProperties,
    testsSourceSnapshotRepository: TestsSourceSnapshotRepository,
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
    private val testSuitesService: TestSuitesService,
    private val executionService: ExecutionService,
) : AbstractStorageWithDatabase<TestsSourceSnapshotDto, TestsSourceSnapshot, TestsSourceSnapshotRepository>(
    Path.of(configProperties.fileStorage.location) / "testSuites", testsSourceSnapshotRepository) {
    private val tmpDir = (Path.of(configProperties.fileStorage.location) / "tmp").createDirectories()

    override fun createNewEntityFromDto(dto: TestsSourceSnapshotDto): TestsSourceSnapshot = dto.toEntity { testSuitesSourceRepository.getByIdOrNotFound(it) }

    override fun findByDto(
        dto: TestsSourceSnapshotDto
    ): TestsSourceSnapshot? = repository.findBySourceIdAndCommitId(
        sourceId = dto.sourceId,
        commitId = dto.commitId,
    )

    override fun beforeDelete(entity: TestsSourceSnapshot) {
        executionService.unlinkTestSuiteFromAllExecution(testSuitesService.getBySourceSnapshot(entity))
    }

    /**
     * @param request
     * @return [TestFilesContent] filled with test files
     */
    fun getTestContent(request: TestFilesRequest): Mono<TestFilesContent> {
        val tmpSourceDir = createTempDirectory(tmpDir, "source-")
        val tmpArchive = createTempFile(tmpSourceDir, "archive-", ARCHIVE_EXTENSION)
        val sourceContent = download(request.testsSourceSnapshot)
            .map { DefaultDataBufferFactory.sharedInstance.wrap(it) }
            .cast(DataBuffer::class.java)

        return DataBufferUtils.write(sourceContent, tmpArchive.outputStream())
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
     * @param testSuitesSource
     * @return true if all [TestsSourceSnapshot] (found by [testSuitesSource]) deleted successfully, otherwise -- false
     */
    fun deleteAll(testSuitesSource: TestSuitesSource): Mono<Boolean> {
        return blockingToFlux { repository.findAllBySource(testSuitesSource) }
            .flatMap { delete(it.toDto()) }
            .all { it }
    }
}
