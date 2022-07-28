package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.ARCHIVE_EXTENSION
import com.saveourtool.save.utils.countPartsTill
import com.saveourtool.save.utils.pathNamesTill
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Component
class TestSuitesSourceSnapshotStorage(
    configProperties: ConfigProperties,
) : AbstractFileBasedStorage<TestSuitesSourceSnapshotKey>(Path.of(configProperties.fileStorage.location) / "testSuites") {
    /**
     * @param rootDir
     * @param pathToContent
     * @return true if there is 4 parts between pathToContent and rootDir and ends with [ARCHIVE_EXTENSION]
     */
    override fun isKey(rootDir: Path, pathToContent: Path): Boolean =
            pathToContent.endsWith(ARCHIVE_EXTENSION) && pathToContent.countPartsTill(rootDir) == PATH_PARTS_COUNT

    @Suppress("MAGIC_NUMBER", "MagicNumber")
    override fun buildKey(rootDir: Path, pathToContent: Path): TestSuitesSourceSnapshotKey {
        val pathNames = pathToContent.pathNamesTill(rootDir)
        return TestSuitesSourceSnapshotKey(
            pathNames[3],
            pathNames[2],
            pathNames[0].dropLast(ARCHIVE_EXTENSION.length),
            pathNames[1].toLong()
        )
    }

    override fun buildPathToContent(rootDir: Path, key: TestSuitesSourceSnapshotKey): Path = with(key) {
        return rootDir / organizationName / testSuitesSourceName / getCreationTimeInMills().toString() / "$version$ARCHIVE_EXTENSION"
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
        .filter { it.organizationName == organizationName && it.testSuitesSourceName == testSuitesSourceName }

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @return max version of existed snapshots
     */
    fun latestVersion(
        organizationName: String,
        testSuitesSourceName: String,
    ): Mono<String> = list()
        .filter { it.organizationName == organizationName && it.testSuitesSourceName == testSuitesSourceName }
        .reduce { max, next ->
            if (max.creationTime > next.creationTime) max else next
        }
        .map { it.version }

    companion object {
        private const val PATH_PARTS_COUNT = 4  // organizationName + testSuitesSourceName + creationTime + version.tar
    }
}
