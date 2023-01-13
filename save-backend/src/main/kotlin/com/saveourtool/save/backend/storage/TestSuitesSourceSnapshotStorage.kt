package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.*
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Component
class TestSuitesSourceSnapshotStorage(
    configProperties: ConfigProperties,
) : AbstractFileBasedStorage<TestSuitesSourceSnapshotKey>(Path.of(configProperties.fileStorage.location) / "testSuites", PATH_PARTS_COUNT) {
    private val tmpDir = (Path.of(configProperties.fileStorage.location) / "tmp").createDirectories()

    /**
     * @param rootDir
     * @param pathToContent
     * @return true if there is 4 parts between pathToContent and rootDir and ends with [ARCHIVE_EXTENSION]
     */
    override fun isKey(rootDir: Path, pathToContent: Path): Boolean = pathToContent.name.endsWith(ARCHIVE_EXTENSION)

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

    private fun String.encodeUrl(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

    private fun String.decodeUrl(): String = URLDecoder.decode(this, StandardCharsets.UTF_8)

    companion object {
        private const val PATH_PARTS_COUNT = 4  // organizationName + testSuitesSourceName + creationTime + version.zip
    }
}
