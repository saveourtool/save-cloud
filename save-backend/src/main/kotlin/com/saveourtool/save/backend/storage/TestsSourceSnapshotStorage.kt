package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceSnapshot.Companion.toEntity
import com.saveourtool.save.storage.AbstractStorageWithDatabase
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.test.TestsSourceSnapshotInfo
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.getByIdOrNotFound
import com.saveourtool.save.utils.switchIfEmptyToNotFound

import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

import java.nio.file.Path

import kotlin.io.path.div

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Component
class TestsSourceSnapshotStorage(
    configProperties: ConfigProperties,
    private val testsSourceSnapshotRepository: TestsSourceSnapshotRepository,
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
) : AbstractStorageWithDatabase<TestsSourceSnapshotDto, TestsSourceSnapshot>(
    Path.of(configProperties.fileStorage.location) / "testSuites", testsSourceSnapshotRepository) {
    override fun createNewEntityFromDto(dto: TestsSourceSnapshotDto): TestsSourceSnapshot = dto.toEntity { testSuitesSourceRepository.getByIdOrNotFound(it) }

    override fun findByDto(
        dto: TestsSourceSnapshotDto
    ): TestsSourceSnapshot? = testsSourceSnapshotRepository.findBySource_IdAndCommitId(dto.sourceId, dto.commitId)

    /**
     * @param snapshotInfo
     * @return true if storage contains some [TestsSourceSnapshotDto] which relates to provided [TestsSourceSnapshotInfo], otherwise -- false
     */
    fun doesContain(
        snapshotInfo: TestsSourceSnapshotInfo,
    ): Mono<Boolean> = blockingToMono {
        testsSourceSnapshotRepository.findBySource_Organization_NameAndSource_NameAndCommitId(snapshotInfo.organizationName, snapshotInfo.sourceName, snapshotInfo.commitId)
    }
        .map { true }
        .defaultIfEmpty(false)

    /**
     * @param snapshotInfo
     * @param content
     * @return uploads [content] with some [TestsSourceSnapshotDto] which relates to provided [TestsSourceSnapshotInfo]
     */
    fun upload(snapshotInfo: TestsSourceSnapshotInfo, content: Flux<ByteBuffer>): Mono<Long> = blockingToMono {
        testSuitesSourceRepository.findByOrganization_NameAndName(snapshotInfo.organizationName, snapshotInfo.sourceName)
    }
        .switchIfEmptyToNotFound {
            "Not found ${TestSuitesSource::class.simpleName} for $snapshotInfo"
        }
        .flatMap { source ->
            val key = TestsSourceSnapshotDto(
                sourceId = source.requiredId(),
                commitId = snapshotInfo.commitId,
                commitTime = snapshotInfo.commitTime,
            )
            super.upload(key, content)
        }
}
