package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceSnapshot.Companion.toEntity
import com.saveourtool.save.entities.TestsSourceVersion
import com.saveourtool.save.storage.AbstractStorageWithDatabase
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.blockingToFlux
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.getByIdOrNotFound

import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import java.nio.file.Path

import kotlin.io.path.div
import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Storage for snapshots of [com.saveourtool.save.entities.TestSuitesSource]
 */
@Component
class TestsSourceSnapshotStorage(
    configProperties: ConfigProperties,
    testSuitesSourceSnapshotRepository: TestSuitesSourceSnapshotRepository,
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
    private val testSuitesSourceVersionRepository: TestSuitesSourceVersionRepository,
    private val testSuitesSourceService: TestSuitesSourceService,
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
) : AbstractStorageWithDatabase<TestsSourceSnapshotDto, TestsSourceSnapshot, TestSuitesSourceSnapshotRepository>(
    Path.of(configProperties.fileStorage.location) / "testSuites", testSuitesSourceSnapshotRepository) {
    override fun createNewEntityFromDto(dto: TestsSourceSnapshotDto): TestsSourceSnapshot = dto.toEntity { testSuitesSourceRepository.getByIdOrNotFound(it) }

    override fun TestsSourceSnapshot.updateByContentSize(sizeBytes: Long): TestsSourceSnapshot {
        // a temporary -- saved version
        testSuitesSourceVersionRepository.save(
            TestsSourceVersion(
                snapshot = this,
                name = this.commitId,
                type = TestSuitesSourceFetchMode.UNKNOWN,
                createdByUser = lnkUserOrganizationRepository.findByOrganization(this.source.organization)
                    .first { it.role == Role.OWNER }.user,
                creationTime = this.commitTime,
            )
        )
        return this
    }

    override fun findByDto(
        repository: TestSuitesSourceSnapshotRepository,
        dto: TestsSourceSnapshotDto
    ): TestsSourceSnapshot? = repository.findBySourceAndCommitId(
        source = testSuitesSourceRepository.getByIdOrNotFound(dto.sourceId),
        commitId = dto.commitId,
    )

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
    ): Mono<TestsSourceSnapshotDto> = blockingToMono {
        val testSuitesSource = testSuitesSourceService.findByName(organizationName, testSuitesSourceName)
        testSuitesSource
            ?.let { testSuitesSourceVersionRepository.findBySnapshot_SourceAndName(it, version) }
            ?.snapshot
            ?.toDto()
    }

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @return list of [TestSuitesSourceSnapshotKey] found by provided values
     */
    fun list(
        organizationName: String,
        testSuitesSourceName: String,
    ): Flux<TestSuitesSourceSnapshotKey> = blockingToFlux {
        val testSuitesSource = testSuitesSourceService.findByName(organizationName, testSuitesSourceName)
        testSuitesSource
            ?.let { testSuitesSourceVersionRepository.findAllBySnapshot_Source(it) }
            ?.map { it.toKey() }
    }

    companion object {
        private fun TestsSourceVersion.toKey() = TestSuitesSourceSnapshotKey(
            organizationName = snapshot.source.organization.name,
            testSuitesSourceName = snapshot.source.name,
            version = name,
            creationTime = creationTime.toKotlinLocalDateTime(),
        )
    }
}
