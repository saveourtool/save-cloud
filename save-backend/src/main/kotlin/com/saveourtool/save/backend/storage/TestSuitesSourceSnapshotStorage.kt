package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.*
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Component
class TestSuitesSourceSnapshotStorage(
    configProperties: ConfigProperties,
) : AbstractFileBasedStorage<TestSuitesSourceSnapshotKey>(
    Path.of(configProperties.fileStorage.location) / "testSuites",
    4,  // organizationName + testSuitesSourceName + creationTime + version.zip
) {
    private val tmpDir = (Path.of(configProperties.fileStorage.location) / "tmp").createDirectories()

    /**
     * @param rootDir
     * @param pathToContent
     * @return true if there is 4 parts between pathToContent and rootDir and ends with [ARCHIVE_EXTENSION]
     */
    override fun isKey(rootDir: Path, pathToContent: Path): Boolean =
            super.isKey(rootDir, pathToContent) && pathToContent.name.endsWith(ARCHIVE_EXTENSION)

    @Suppress("DestructuringDeclarationWithTooManyEntries")
    override fun buildKey(rootDir: Path, pathToContent: Path): TestSuitesSourceSnapshotKey {
        val (version, creationTime, sourceName, organizationName) = pathToContent.pathNamesTill(rootDir)
        return TestSuitesSourceSnapshotKey(
            organizationName,
            sourceName.decodeUrl(),
            version.dropLast(ARCHIVE_EXTENSION.length),
            creationTime.toLong()
        )
    }

    override fun buildPathToContent(rootDir: Path, key: TestSuitesSourceSnapshotKey): Path = with(key) {
        return rootDir / organizationName / testSuitesSourceName.encodeUrl() / creationTimeInMills.toString() / "$version$ARCHIVE_EXTENSION"
    }

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
     * @return key which contains provided values
     */
    fun findKey(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
    ): Mono<TestSuitesSourceSnapshotKey> = list()
        .filter { it.equalsTo(organizationName, testSuitesSourceName, version) }
        .singleOrEmpty()

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @return list of [TestSuitesSourceSnapshotKey] found by provided values
     */
    fun list(
        organizationName: String,
        testSuitesSourceName: String,
    ): Flux<TestSuitesSourceSnapshotKey> = list()
        .filter { it.equalsTo(organizationName, testSuitesSourceName) }

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

    private fun String.encodeUrl(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

    private fun String.decodeUrl(): String = URLDecoder.decode(this, StandardCharsets.UTF_8)
}
