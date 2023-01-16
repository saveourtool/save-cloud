package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.storage.AbstractMigrationStorage
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.getByIdOrNotFound
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import javax.annotation.PostConstruct

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Service
class MigrationTestSuitesSourceSnapshotStorage(
    oldStorage: TestSuitesSourceSnapshotStorage,
    private val newStorage: TestsSourceSnapshotStorage,
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
    ): Mono<Boolean> = newStorage.doesContain(organizationName, testSuitesSourceName, version)

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
    ): Flux<ByteBuffer> = newStorage.downloadByVersion(organizationName, testSuitesSourceName, version)

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
    ): Mono<Boolean> = newStorage.deleteByVersion(organizationName, testSuitesSourceName, version)

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @return list of [TestSuitesSourceSnapshotKey] found by provided values
     */
    fun list(
        organizationName: String,
        testSuitesSourceName: String,
    ): Flux<TestSuitesSourceSnapshotKey> = newStorage.list(organizationName, testSuitesSourceName)
}
