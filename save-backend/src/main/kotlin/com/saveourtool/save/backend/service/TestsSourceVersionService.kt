package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.storage.MigrationTestsSourceSnapshotStorage
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.ARCHIVE_EXTENSION
import com.saveourtool.save.utils.extractZipHere
import com.saveourtool.save.utils.orNotFound
import org.springframework.context.annotation.Lazy
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Service
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
    configProperties: ConfigProperties,
) {
    private val tmpDir = (java.nio.file.Path.of(configProperties.fileStorage.location) / "tmp").createDirectories()

    /**
     * @param versionInfo
     * @param content
     * @return count of written bytes
     */
    fun upload(
        versionInfo: TestsSourceVersionInfo,
        content: Flux<ByteBuffer>,
    ): Mono<Long> = snapshotStorage.upload(
        key = TestSuitesSourceSnapshotKey(
            organizationName = versionInfo.organizationName,
            testSuitesSourceName = versionInfo.sourceName,
            version = versionInfo.version,
            creationTime = versionInfo.commitTime,
        ),
        content = content
    )

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
    ): Mono<Boolean> = findSnapshotKey(organizationName, sourceName, version)
        .map { true }
        .defaultIfEmpty(false)

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @param version
     * @return content of a key which contains provided values
     */
    fun download(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
    ): Flux<ByteBuffer> = findSnapshotKey(
        organizationName = organizationName,
        sourceName = testSuitesSourceName,
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
    ): Mono<Boolean> = findSnapshotKey(
        organizationName = organizationName,
        sourceName = sourceName,
        version = version,
    ).flatMap { snapshotStorage.delete(it) }

    private fun findSnapshotKey(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Mono<TestSuitesSourceSnapshotKey> = snapshotStorage.list()
        .filter { it.equalsTo(organizationName, sourceName, version) }
        .singleOrEmpty()

    /**
     * @param organizationName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun list(
        organizationName: String,
    ): Flux<TestsSourceVersionInfo> = snapshotStorage.list()
        .filter { it.organizationName == organizationName }
        .map { it.toVersionInfo() }

    /**
     * @param organizationName
     * @param sourceName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun list(
        organizationName: String,
        sourceName: String,
    ): Flux<TestsSourceVersionInfo> = snapshotStorage.list()
        .filter { it.equalsTo(organizationName, sourceName) }
        .map { it.toVersionInfo() }

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
    ): Mono<Unit> = snapshotStorage.list()
        .filter { it.equalsTo(organizationName, oldSourceName) }
        .map { it to it.copy(testSuitesSourceName = newSourceName) }
        .flatMap { (sourceKey, targetKey) ->
            snapshotStorage.move(sourceKey, targetKey)
        }
        .then(Mono.just(Unit))

    companion object {
        private fun TestSuitesSourceSnapshotKey.toVersionInfo() = TestsSourceVersionInfo(
            organizationName = organizationName,
            sourceName = testSuitesSourceName,
            version = version,
            creationTime = convertAndGetCreationTime(),
            commitId = version,
            commitTime = convertAndGetCreationTime(),
        )
    }
}
