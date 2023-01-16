package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.storage.AbstractMigrationStorage
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.getByIdOrNotFound
import org.springframework.stereotype.Service
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
}