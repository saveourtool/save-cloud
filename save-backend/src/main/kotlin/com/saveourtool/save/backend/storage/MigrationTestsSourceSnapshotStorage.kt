package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.backend.repository.TestsSourceVersionRepository
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.entities.TestsSourceVersion
import com.saveourtool.save.request.TestFilesRequest
import com.saveourtool.save.storage.AbstractMigrationStorage
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import javax.annotation.PostConstruct

import kotlinx.datetime.toKotlinLocalDateTime
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import kotlin.io.path.*

/**
 * Storage for [com.saveourtool.save.entities.TestsSourceSnapshot]
 */
@Service
class MigrationTestsSourceSnapshotStorage(
    oldStorage: TestSuitesSourceSnapshotStorage,
    newStorage: TestsSourceSnapshotStorage,
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
    private val testsSourceVersionRepository: TestsSourceVersionRepository,
    private val testSuitesSourceService: TestSuitesSourceService,
    configProperties: ConfigProperties,
) : AbstractMigrationStorage<TestSuitesSourceSnapshotKey, TestsSourceSnapshotDto>(oldStorage, newStorage) {
    private val tmpDir = (java.nio.file.Path.of(configProperties.fileStorage.location) / "tmp").createDirectories()

    /**
     * A temporary init method which copies file from one storage to another
     */
    @PostConstruct
    fun init() {
        super.migrate()
    }

    override fun TestsSourceSnapshotDto.toOldKey(): TestSuitesSourceSnapshotKey {
        val source = testSuitesSourceRepository.getByIdOrNotFound(sourceId)
        return TestSuitesSourceSnapshotKey(
            organizationName = source.organization.name,
            testSuitesSourceName = source.name,
            version = commitId,
            creationTime = commitTime,
        )
    }

    override fun TestSuitesSourceSnapshotKey.toNewKey(): TestsSourceSnapshotDto {
        val source = testSuitesSourceService.getByName(organizationName, testSuitesSourceName)
        return TestsSourceSnapshotDto(
            sourceId = source.requiredId(),
            commitId = version,
            commitTime = convertAndGetCreationTime(),
        )
    }

    override fun upload(key: TestSuitesSourceSnapshotKey, content: Flux<ByteBuffer>): Mono<Long> = super.upload(key, content).doOnNext { writtenBytes ->
        log.info {
            "Saved ${key.toLogString(writtenBytes)}"
        }
    }

    override fun copy(source: TestSuitesSourceSnapshotKey, target: TestSuitesSourceSnapshotKey): Mono<Long> = super.copy(source, target).doOnNext { writtenBytes ->
        log.info {
            "Copied ${source.toLogString(writtenBytes)} to new version ${target.version}"
        }
    }

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return [TestSuitesSourceSnapshotKey] related to some [com.saveourtool.save.test.TestsSourceVersionDto] with provided values
     */
    fun findSnapshotKey(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Mono<TestSuitesSourceSnapshotKey> = validateAndRun { doFindSnapshotKey(organizationName, sourceName, version) }

    private fun doFindSnapshotKey(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Mono<TestSuitesSourceSnapshotKey> = blockingToMono {
        testsSourceVersionRepository.findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(
            organizationName, sourceName, version
        )
            ?.snapshot
            ?.let { snapshot ->
                TestSuitesSourceSnapshotKey(
                    organizationName = snapshot.source.organization.name,
                    testSuitesSourceName = snapshot.source.name,
                    version = snapshot.commitId,
                    creationTime = snapshot.commitTime.toKotlinLocalDateTime(),
                )
            }
    }

    /**
     * @param organizationName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun listAsVersionInfo(
        organizationName: String,
    ): Flux<TestsSourceVersionInfo> = validateAndRun { doListAsVersionInfo(organizationName) }

    private fun doListAsVersionInfo(
        organizationName: String,
    ): Flux<TestsSourceVersionInfo> = blockingToFlux {
        testsSourceVersionRepository.findAllBySnapshot_Source_Organization_Name(organizationName)
            .map(TestsSourceVersion::toInfo)
    }

    /**
     * @param organizationName
     * @param sourceName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun listAsVersionInfo(
        organizationName: String,
        sourceName: String,
    ): Flux<TestsSourceVersionInfo> = validateAndRun { doListAsVersionInfo(organizationName, sourceName) }

    private fun doListAsVersionInfo(
        organizationName: String,
        sourceName: String,
    ): Flux<TestsSourceVersionInfo> = blockingToFlux {
        testsSourceVersionRepository.findAllBySnapshot_Source_Organization_NameAndSnapshot_Source_Name(
            organizationName,
            sourceName
        )
            .map(TestsSourceVersion::toInfo)
    }


    /**
     * @param request
     * @return [TestFilesContent] filled with test files
     */
    fun getTestContent(request: TestFilesRequest): Mono<TestFilesContent> = validateAndRun {
            val tmpSourceDir = createTempDirectory(tmpDir, "source-")
            val tmpArchive = createTempFile(tmpSourceDir, "archive-", ARCHIVE_EXTENSION)
            val sourceContent = download(request.storageKey)
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
        private val log: Logger = getLogger<MigrationTestsSourceSnapshotStorage>()

        private fun TestSuitesSourceSnapshotKey.toLogString(writtenBytes: Long) =
                "($writtenBytes bytes) snapshot of $testSuitesSourceName in $organizationName with version $version"
    }
}
