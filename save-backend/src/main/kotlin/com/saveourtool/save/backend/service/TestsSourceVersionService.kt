package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestsSourceSnapshotRepository
import com.saveourtool.save.backend.repository.TestsSourceVersionRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.storage.TestsSourceSnapshotStorage
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceVersion
import com.saveourtool.save.entities.TestsSourceVersion.Companion.toEntity
import com.saveourtool.save.test.*
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.utils.*
import org.slf4j.Logger

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Service for [TestsSourceVersionInfo]
 */
@Service
class TestsSourceVersionService(
    @Lazy
    private val testSuitesService: TestSuitesService,
    private val snapshotRepository: TestsSourceSnapshotRepository,
    private val versionRepository: TestsSourceVersionRepository,
    private val userRepository: UserRepository,
    private val snapshotStorage: TestsSourceSnapshotStorage,
) {
    /**
     * @param sourceId ID of [com.saveourtool.save.entities.TestSuitesSource]
     * @param commitId [TestsSourceSnapshot.commitId]
     * @return [TestsSourceSnapshotDto] found by provided values
     */
    fun findSnapshot(
        sourceId: Long,
        commitId: String,
    ): TestsSourceSnapshotDto? = snapshotRepository.findBySourceIdAndCommitId(sourceId, commitId)?.toDto()

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return [TestsSourceVersionDto] found by provided values
     */
    fun findVersion(
        organizationName: String,
        sourceName: String,
        version: String,
    ): TestsSourceVersionDto? = versionRepository.findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(organizationName, sourceName, version)
        ?.toDto()

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return [TestsSourceSnapshotDto] found by provided values
     */
    fun findSnapshot(
        organizationName: String,
        sourceName: String,
        version: String,
    ): TestsSourceSnapshotDto? = versionRepository.findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(organizationName, sourceName, version)
        ?.snapshot
        ?.toDto()

    /**
     * @param organizationName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun getAllAsInfo(
        organizationName: String,
    ): List<TestsSourceVersionInfo> = versionRepository.findAllBySnapshot_Source_Organization_Name(organizationName)
        .map(TestsSourceVersion::toInfo)

    /**
     * @param organizationName
     * @param sourceName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun getAllAsInfo(
        organizationName: String,
        sourceName: String,
    ): List<TestsSourceVersionInfo> =
            versionRepository.findAllBySnapshot_Source_Organization_NameAndSnapshot_Source_Name(organizationName, sourceName)
                .map(TestsSourceVersion::toInfo)

    /**
     * @param organizationName
     * @param sourceName
     * @return all fetched version of [TestsSourceSnapshot] found by provided values
     */
    fun getAllVersions(
        organizationName: String,
        sourceName: String,
    ): Set<String> = versionRepository.findAllBySnapshot_Source_Organization_NameAndSnapshot_Source_Name(
        organizationName,
        sourceName
    ).mapTo(HashSet(), TestsSourceVersion::name)

    /**
     * @param sourceId
     * @param version
     * @return [TestsSourceVersionDto] found by provided values
     */
    fun find(
        sourceId: Long,
        version: String,
    ): TestsSourceVersionDto? = versionRepository.findBySnapshot_SourceIdAndName(sourceId, version)?.toDto()

    /**
     * @param info
     * @return [TestsSourceSnapshotDto] found by [TestsSourceVersionInfo]
     */
    fun findByInfo(
        info: TestsSourceVersionInfo,
    ): TestsSourceVersionDto? = versionRepository.findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(
        organizationName = info.organizationName,
        sourceName = info.sourceName,
        version = info.version,
    )?.toDto()

    /**
     * @param dto
     * @return [TestsSourceSnapshotDto] found by [TestsSourceVersionInfo]
     */
    fun findSnapshot(
        dto: TestsSourceVersionDto,
    ): TestsSourceSnapshotDto? = doFind(dto)?.snapshot?.toDto()

    private fun doFind(
        dto: TestsSourceVersionDto,
    ): TestsSourceVersion? = versionRepository.findBySnapshot_IdAndName(dto.snapshotId, dto.name)

    /**
     * Deletes [TestsSourceVersionDto] and [TestsSourceSnapshotDto] if there are no another [TestsSourceVersionDto] related to it
     *
     * @param version [TestsSourceVersionDto]
     * @return true if [TestsSourceSnapshot] related to deleted [TestsSourceVersion] doesn't have another [TestsSourceVersion] related to it
     */
    @Transactional
    fun delete(
        version: TestsSourceVersionDto
    ) {
        val versionEntity =
            versionRepository.findBySnapshot_IdAndName(version.snapshotId, version.name)
                .orNotFound {
                    "Not found ${TestsSourceVersion::class.simpleName} for $version"
                }
        doDelete(versionEntity)
    }

    /**
     * Deletes [TestsSourceVersionDto] and [TestsSourceSnapshotDto] if there are no another [TestsSourceVersionDto] related to it
     *
     * @param organizationName
     * @param sourceName
     * @param version
     */
    @Transactional
    fun delete(
        organizationName: String,
        sourceName: String,
        version: String,
    ) {
        val versionEntity =
                versionRepository.findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(
                    organizationName = organizationName,
                    sourceName = sourceName,
                    version = version,
                ).orNotFound {
                    "Not found ${TestsSourceVersion::class.simpleName} with version $version in $organizationName/$sourceName"
                }
        doDelete(versionEntity)
    }

    private fun doDelete(versionEntity: TestsSourceVersion) {
        // clean-up [TestsSuite]
        testSuitesService.deleteTestSuite(versionEntity.snapshot.source, versionEntity.name)
        versionRepository.delete(versionEntity)
        val snapshotEntity = versionEntity.snapshot
        if (versionRepository.findAllBySnapshot(snapshotEntity).isEmpty()) {
            val snapshot = snapshotEntity.toDto()
            snapshotStorage.delete(snapshot)
                .map { deleted ->
                    if (!deleted) {
                        log.warn {
                            "Failed to delete snapshot: $snapshot"
                        }
                    }
                }
                .subscribe()
        }
    }

    /**
     * Saves [TestsSourceVersion] created from provided [TestsSourceVersionDto]
     *
     * @param dto
     */
    @Transactional
    fun save(
        dto: TestsSourceVersionDto,
    ) {
        val entity = dto.toEntity(
            snapshotResolver = snapshotRepository::getByIdOrNotFound,
            userResolver = userRepository::getByIdOrNotFound
        )
        val savedEntity = versionRepository.save(entity)
        // copy test suites
        testSuitesService.copyToNewVersion(
            sourceId = savedEntity.snapshot.source.requiredId(),
            originalVersion = savedEntity.snapshot.commitId,
            newVersion = savedEntity.name,
        )
    }

    private fun getSnapshot(dto: TestsSourceSnapshotDto) = snapshotRepository.findBySourceIdAndCommitId(
        sourceId = dto.sourceId,
        commitId = dto.commitId,
    ).orNotFound {
        "Not found ${TestsSourceSnapshot::class.simpleName} for $dto"
    }

    companion object {
        private val log: Logger = getLogger<TestsSourceVersionService>()
    }
}
