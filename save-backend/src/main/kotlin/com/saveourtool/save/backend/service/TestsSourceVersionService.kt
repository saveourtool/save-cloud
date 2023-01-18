package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestsSourceSnapshotRepository
import com.saveourtool.save.backend.repository.TestsSourceVersionRepository
import com.saveourtool.save.backend.storage.MigrationTestsSourceSnapshotStorage
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.backend.storage.TestsSourceSnapshotStorage
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceVersion
import com.saveourtool.save.entities.User
import com.saveourtool.save.test.*
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.*
import kotlinx.datetime.toJavaLocalDateTime
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.LocalDateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import kotlin.io.path.*

/**
 * Service for [TestsSourceVersionInfo]
 */
@Service
class TestsSourceVersionService(
    @Lazy
    private val migrationStorage: MigrationTestsSourceSnapshotStorage,
    private val snapshotStorage: TestsSourceSnapshotStorage,
    private val testsSourceSnapshotRepository: TestsSourceSnapshotRepository,
    private val testsSourceVersionRepository: TestsSourceVersionRepository,
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
    ): Mono<Boolean> {
        require(migrationStorage.isMigrated())
        return findSnapshotKey(organizationName, sourceName, version)
            .map { true }
            .defaultIfEmpty(false)
    }

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
    ): Flux<ByteBuffer> {
        require(migrationStorage.isMigrated())
        return findSnapshotKey(
            organizationName = organizationName,
            sourceName = sourceName,
            version = version,
        ).flatMapMany { snapshotStorage.download(it) }
    }

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
    ): Mono<Boolean> {
        require(migrationStorage.isMigrated())
        return findSnapshotKey(
            organizationName = organizationName,
            sourceName = sourceName,
            version = version,
        ).flatMap { snapshotStorage.delete(it) }
    }

    private fun findSnapshotKey(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Mono<TestsSourceSnapshotDto> = blockingToMono {
        testsSourceVersionRepository.findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(
            organizationName, sourceName, version
        )?.snapshot?.toDto()
    }

    /**
     * @param organizationName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun list(
        organizationName: String,
    ): Flux<TestsSourceVersionInfo> {
        require(migrationStorage.isMigrated())
        return blockingToFlux {
            testsSourceVersionRepository.findAllBySnapshot_Source_Organization_Name(organizationName)
                .map(TestsSourceVersion::toInfo)
        }
    }

    /**
     * @param organizationName
     * @param sourceName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun list(
        organizationName: String,
        sourceName: String,
    ): Flux<TestsSourceVersionInfo> {
        require(migrationStorage.isMigrated())
        return blockingToFlux {
            testsSourceVersionRepository.findAllBySnapshot_Source_Organization_NameAndSnapshot_Source_Name(organizationName, sourceName)
                .map(TestsSourceVersion::toInfo)
        }
    }

    /**
     * @param request
     * @return [TestFilesContent] filled with test files
     */
    fun getTestContent(request: TestFilesRequest): Mono<TestFilesContent> = with(request.testSuitesSource) {
        findSnapshotKey(organizationName, name, request.version)
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
     * Handle changing source name -- files are moved in snapshot storage
     *
     * @param organizationName
     * @param oldSourceName
     * @param newSourceName
     * @return [Mono] without value
     */
    fun updateSourceName(
        organizationName: String,
        oldSourceName: String,
        newSourceName: String,
    ): Mono<Unit> {
        require(migrationStorage.isMigrated())
        // no need to move files in new storage
        return Mono.just(Unit)
    }

    /**
     * Saves [TestsSourceVersion] created from provided [TestsSourceVersionInfo]
     *
     * @param versionInfo
     */
    fun save(
        versionInfo: TestsSourceVersionInfo,
    ) {
        testsSourceVersionRepository.save(
            TestsSourceVersion(
                snapshot = testsSourceSnapshotRepository.findBySource_Organization_NameAndSource_NameAndCommitId(
                    organizationName = versionInfo.organizationName,
                    sourceName = versionInfo.sourceName,
                    commitId = versionInfo.commitId,
                ).orNotFound {
                    "Not found ${TestsSourceSnapshot::class.simpleName} for ${versionInfo.snapshotInfo}"
                },
                name = versionInfo.version,
                creationTime = versionInfo.creationTime.toJavaLocalDateTime()
            )
        )
    }

    companion object {
        private fun TestSuitesSourceSnapshotKey.toVersionInfo() = TestsSourceVersionInfo(
            organizationName = organizationName,
            sourceName = testSuitesSourceName,
            commitId = version,
            commitTime = convertAndGetCreationTime(),
            version = version,
            creationTime = convertAndGetCreationTime(),
        )
    }
}
