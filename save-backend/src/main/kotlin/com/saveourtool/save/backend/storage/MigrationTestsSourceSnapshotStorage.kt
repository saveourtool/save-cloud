package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.storage.AbstractMigrationStorage
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.getByIdOrNotFound
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import javax.annotation.PostConstruct

/**
 * Storage for [com.saveourtool.save.entities.TestsSourceSnapshot]
 */
@Service
class MigrationTestsSourceSnapshotStorage(
    oldStorage: TestSuitesSourceSnapshotStorage,
    newStorage: TestsSourceSnapshotStorage,
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
    private val testSuitesSourceService: TestSuitesSourceService,
) : AbstractMigrationStorage<TestSuitesSourceSnapshotKey, TestsSourceSnapshotDto>(oldStorage, newStorage) {
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

    companion object {
        private val log: Logger = getLogger<MigrationTestsSourceSnapshotStorage>()

        private fun TestSuitesSourceSnapshotKey.toLogString(writtenBytes: Long) =
                "($writtenBytes bytes) snapshot of $testSuitesSourceName in $organizationName with version $version"
    }
}
