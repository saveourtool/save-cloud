package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.*
import org.springframework.stereotype.Component
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
            version.dropLast(ARCHIVE_EXTENSION.length).decodeUrl(),
            creationTime.toLong()
        )
    }

    override fun buildPathToContent(rootDir: Path, key: TestSuitesSourceSnapshotKey): Path = with(key) {
        return rootDir / organizationName / testSuitesSourceName.encodeUrl() / creationTimeInMills.toString() / "${version.encodeUrl()}$ARCHIVE_EXTENSION"
    }

    private fun String.encodeUrl(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

    private fun String.decodeUrl(): String = URLDecoder.decode(this, StandardCharsets.UTF_8)

    companion object {
        private const val PATH_PARTS_COUNT = 4  // organizationName + testSuitesSourceName + creationTime + version.zip
    }
}
