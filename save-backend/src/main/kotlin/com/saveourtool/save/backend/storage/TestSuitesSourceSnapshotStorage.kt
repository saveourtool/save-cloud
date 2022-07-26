package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.TAR_EXTENSION
import com.saveourtool.save.utils.countPartsTill
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name

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
     * @return true if there is 4 parts between pathToContent and rootDir and ends with [TAR_EXTENSION]
     */
    override fun isKey(rootDir: Path, pathToContent: Path): Boolean =
            pathToContent.endsWith(TAR_EXTENSION) && pathToContent.countPartsTill(rootDir) == PATH_PARTS_COUNT

    override fun buildKey(rootDir: Path, pathToContent: Path): TestSuitesSourceSnapshotKey =
            TestSuitesSourceSnapshotKey(
                pathToContent.parent.parent.parent.name,
                pathToContent.parent.parent.name,
                pathToContent.name.dropLast(TAR_EXTENSION.length),
                pathToContent.parent.name.toLong()
            )

    override fun buildPathToContent(rootDir: Path, key: TestSuitesSourceSnapshotKey): Path = with(key) {
        return rootDir / organizationName / testSuitesSourceName / getCreationTimeInMills().toString() / "$version$TAR_EXTENSION"
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
    ): Mono<Boolean> = list()
        .filter { it.organizationName == organizationName && it.testSuitesSourceName == testSuitesSourceName && it.version == version }
        .singleOrEmpty()
        .map { true }
        .defaultIfEmpty(false)

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
        private const val PATH_PARTS_COUNT = 4 // organizationName + testSuitesSourceName + creationTime + version.tar
    }
}
