package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.backend.repository.TestSuitesSourceSnapshotRepository
import com.saveourtool.save.backend.repository.TestSuitesSourceVersionRepository
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.entities.TestSuitesSourceSnapshot
import com.saveourtool.save.entities.TestSuitesSourceSnapshot.Companion.toEntity
import com.saveourtool.save.entities.TestSuitesSourceVersion
import com.saveourtool.save.storage.AbstractStorageWithDatabase
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.*

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import java.nio.file.Path

import kotlin.io.path.*
import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Component
class NewTestSuitesSourceSnapshotStorage(
    configProperties: ConfigProperties,
    testSuitesSourceSnapshotRepository: TestSuitesSourceSnapshotRepository,
    private val testSuitesSourceVersionRepository: TestSuitesSourceVersionRepository,
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
    private val testSuitesSourceService: TestSuitesSourceService,
) : AbstractStorageWithDatabase<TestSuitesSourceSnapshotDto, TestSuitesSourceSnapshot, TestSuitesSourceSnapshotRepository>(
    Path.of(configProperties.fileStorage.location) / "testSuites", testSuitesSourceSnapshotRepository) {
    private val tmpDir = (Path.of(configProperties.fileStorage.location) / "tmp").createDirectories()

    override fun createNewEntityFromDto(dto: TestSuitesSourceSnapshotDto): TestSuitesSourceSnapshot =
            dto.toEntity { testSuitesSourceRepository.getByIdOrNotFound(it) }

    override fun findByDto(
        repository: TestSuitesSourceSnapshotRepository,
        dto: TestSuitesSourceSnapshotDto
    ): TestSuitesSourceSnapshot? = repository.findBySourceAndCommitId(
        testSuitesSource = testSuitesSourceRepository.getByIdOrNotFound(dto.sourceId),
        commitId = dto.commitId,
    )

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @param version
     * @return true if storage contains snapshot with provided values, otherwise -- false
     */
    fun doesContain(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
    ): Mono<Boolean> = findKey(organizationName, testSuitesSourceName, version)
        .map { true }
        .defaultIfEmpty(false)

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @param version
     * @return content of a key which contains provided values
     */
    fun downloadByVersion(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
    ): Flux<ByteBuffer> = findKey(
        organizationName = organizationName,
        testSuitesSourceName = testSuitesSourceName,
        version = version,
    ).flatMapMany { download(it) }

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @param version
     * @return result of deletion of a key which contains provided values
     */
    fun deleteByVersion(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
    ): Mono<Boolean> = findKey(
        organizationName = organizationName,
        testSuitesSourceName = testSuitesSourceName,
        version = version,
    ).flatMap { delete(it) }

    private fun findKey(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
    ): Mono<TestSuitesSourceSnapshotDto> = blockingToMono {
        val testSuitesSource = testSuitesSourceService.findByName(organizationName, testSuitesSourceName)
        testSuitesSource
            ?.let { testSuitesSourceVersionRepository.findBySnapshot_SourceAndName(it, version) }
            ?.snapshot
            ?.toDto()
    }

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @return list of [TestSuitesSourceSnapshotKey] found by provided values
     */
    fun list(
        organizationName: String,
        testSuitesSourceName: String,
    ): Flux<TestSuitesSourceSnapshotKey> = blockingToFlux {
        val testSuitesSource = testSuitesSourceService.findByName(organizationName, testSuitesSourceName)
        testSuitesSource
            ?.let { testSuitesSourceVersionRepository.findAllBySnapshot_Source(it) }
            ?.map { it.toKey() }
    }

    /**
     * @param request
     * @return [TestFilesContent] filled with test files
     */
    fun getTestContent(request: TestFilesRequest): Mono<TestFilesContent> = with(request.testSuitesSource) {
        findKey(organizationName, name, request.version)
            .orNotFound {
                "There is no content for tests from $name in $organizationName with version ${request.version}"
            }
    }
        .flatMap { key ->
            val tmpSourceDir = createTempDirectory(tmpDir, "source-")
            val tmpArchive = createTempFile(tmpSourceDir, "archive-", ARCHIVE_EXTENSION)
            val sourceContent = download(key)
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

    companion object {
        private fun TestSuitesSourceVersion.toKey() = TestSuitesSourceSnapshotKey(
            organizationName = snapshot.source.organization.name,
            testSuitesSourceName = snapshot.source.name,
            version = name,
            creationTime = creationTime.toKotlinLocalDateTime(),
        )
    }
}
