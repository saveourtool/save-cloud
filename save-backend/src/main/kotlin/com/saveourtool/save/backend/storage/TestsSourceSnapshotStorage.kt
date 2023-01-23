package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceSnapshot.Companion.toEntity
import com.saveourtool.save.request.TestFilesRequest
import com.saveourtool.save.storage.AbstractStorageWithDatabase
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.utils.ARCHIVE_EXTENSION
import com.saveourtool.save.utils.extractZipHere
import com.saveourtool.save.utils.getByIdOrNotFound

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
}
