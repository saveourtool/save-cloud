package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage.Companion.formatCreationTime
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.TAR_EXTENSION
import com.saveourtool.save.utils.countPartsTill
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.extra.math.max
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
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
                pathToContent.parent.name.parseCreationTime()
            )

    override fun buildPathToContent(rootDir: Path, key: TestSuitesSourceSnapshotKey): Path = with(key) {
        return rootDir / organizationName / testSuitesSourceName / creationTime.formatCreationTime() / "$version$TAR_EXTENSION"
    }

    fun doesContain(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
    ): Mono<Boolean> = list()
        .filter { it.organizationName == organizationName && it.testSuitesSourceName == testSuitesSourceName && it.version == version }
        .singleOrEmpty()
        .map { true }
        .switchIfEmpty(Mono.just(false))

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
        private val creationTimeZoneId = ZoneOffset.UTC

        private fun String.parseCreationTime() = this.toLong()
            .let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), creationTimeZoneId) }
            .toKotlinLocalDateTime()

        private fun kotlinx.datetime.LocalDateTime.formatCreationTime() = toJavaLocalDateTime()
            .toInstant(creationTimeZoneId)
            .toEpochMilli()
            .toString()
    }
}
