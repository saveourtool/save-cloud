package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.request.TestFilesRequest
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.ReactiveStorageWithDatabase
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.utils.*
import okio.Path.Companion.toOkioPath

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

import kotlin.io.path.*

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Component
class TestsSourceSnapshotStorage(
    s3Operations: S3Operations,
    testsSourceSnapshotRepository: TestsSourceSnapshotRepository,
    s3KeyManager: TestsSourceSnapshotS3KeyManager,
) : ReactiveStorageWithDatabase<TestsSourceSnapshotDto, TestsSourceSnapshot, TestsSourceSnapshotRepository, TestsSourceSnapshotS3KeyManager>(
    s3Operations,
    s3KeyManager,
    testsSourceSnapshotRepository,
) {
    /**
     * @param request
     * @return [TestFilesContent] filled with test files
     */
    fun getTestContent(request: TestFilesRequest): Mono<TestFilesContent> {
        val tmpSourceDir = createTempDirectory("source-")
        val tmpArchive = createTempFile(tmpSourceDir, "archive-", ARCHIVE_EXTENSION)
        val sourceContent = download(request.testsSourceSnapshot)
            .map { DefaultDataBufferFactory.sharedInstance.wrap(it) }
            .cast(DataBuffer::class.java)

        return DataBufferUtils.write(sourceContent, tmpArchive.outputStream())
            .map { DataBufferUtils.release(it) }
            .collectList()
            .map {
                tmpArchive.toOkioPath().extractZipHere()
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
    fun deleteAll(testSuitesSource: TestSuitesSource): Mono<Boolean> = blockingToFlux { s3KeyManager.findAll(testSuitesSource) }
        .flatMap { delete(it) }
        .all { it }
}
