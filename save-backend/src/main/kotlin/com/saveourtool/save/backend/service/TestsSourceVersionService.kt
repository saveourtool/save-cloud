package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestsSourceSnapshotRepository
import com.saveourtool.save.backend.repository.TestsSourceVersionRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceVersion
import com.saveourtool.save.entities.TestsSourceVersion.Companion.toEntity
import com.saveourtool.save.test.*
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.utils.*
import kotlinx.datetime.toKotlinLocalDateTime

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for [TestsSourceVersionInfo]
 */
@Service
class TestsSourceVersionService(
    @Lazy
    private val testSuitesService: TestSuitesService,
    private val testsSourceSnapshotRepository: TestsSourceSnapshotRepository,
    private val testsSourceVersionRepository: TestsSourceVersionRepository,
    private val userRepository: UserRepository,
) {
    fun getAllAsInfo(): Collection<TestsSourceVersionInfo> = testsSourceVersionRepository.findAll()
        .map(TestsSourceVersion::toInfo)

    fun getAllAsInfo(organizationName: String): Collection<TestsSourceVersionInfo> = testsSourceVersionRepository.findAllBySnapshot_Source_Organization_Name(organizationName)
        .map(TestsSourceVersion::toInfo)

    fun getAllAsInfo(organizationName: String, sourceName: String): Collection<TestsSourceVersionInfo> = testsSourceVersionRepository.findAllBySnapshot_Source_Organization_NameAndSnapshot_Source_Name(
        organizationName,
        sourceName
    ).map(TestsSourceVersion::toInfo)

    fun getAllVersions(organizationName: String, sourceName: String): Set<String> = testsSourceVersionRepository.findAllBySnapshot_Source_Organization_NameAndSnapshot_Source_Name(
        organizationName,
        sourceName
    ).mapTo(HashSet(), TestsSourceVersion::name)

    fun find(
        sourceId: Long,
        version: String,
    ): TestsSourceVersionDto? = testsSourceVersionRepository.findBySnapshot_SourceIdAndName(sourceId, version)?.toDto()

    fun getStorageKey(
        sourceId: Long,
        version: String,
    ): TestSuitesSourceSnapshotKey = testsSourceVersionRepository
        .findBySnapshot_SourceIdAndName(sourceId, version)
        ?.let { sourceVersion ->
            with(sourceVersion.snapshot) {
                TestSuitesSourceSnapshotKey(
                    organizationName = source.organization.name,
                    testSuitesSourceName = source.name,
                    version = commitId,
                    creationTime = commitTime.toKotlinLocalDateTime(),
                )
            }
        }
        .orNotFound {
            "Failed to get ${TestSuitesSourceSnapshotKey::class.java} by sourceId $sourceId with version $version"
        }

    fun findByInfo(
        info: TestsSourceVersionInfo,
    ): TestsSourceVersionDto? = testsSourceVersionRepository.findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(
        organizationName = info.organizationName,
        sourceName = info.sourceName,
        version = info.version,
    )?.toDto()

    fun findSnapshot(
        dto: TestsSourceVersionDto,
    ): TestsSourceSnapshotDto? = doFind(dto)?.snapshot?.toDto()

    private fun doFind(
        dto: TestsSourceVersionDto,
    ): TestsSourceVersion? = testsSourceVersionRepository.findBySnapshot_IdAndName(dto.snapshot.sourceId, dto.name)


    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return true if [TestsSourceSnapshot] related to deleted [TestsSourceVersion] doesn't have another [TestsSourceVersion] related to it
     */
    fun delete(
        organizationName: String,
        sourceName: String,
        version: String,
    ): Boolean {
        val versionEntity =
            testsSourceVersionRepository.findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(
                organizationName = organizationName,
                sourceName = sourceName,
                version = version,
            ).orNotFound {
                "Not found ${TestsSourceVersion::class.simpleName} with version $version in $organizationName/$sourceName"
            }
        testsSourceVersionRepository.delete(versionEntity)
        val snapshot = versionEntity.snapshot
        return testsSourceVersionRepository.findAllBySnapshot(snapshot).isEmpty()
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
            snapshotResolver = ::getSnapshot,
            userResolver = userRepository::getByIdOrNotFound
        )
        val savedEntity = testsSourceVersionRepository.save(entity)
        // copy test suites
        testSuitesService.copyToNewVersion(
            sourceId = savedEntity.snapshot.source.requiredId(),
            originalVersion = savedEntity.snapshot.commitId,
            newVersion = savedEntity.name,
        )
    }

    private fun getSnapshot(dto: TestsSourceSnapshotDto) = testsSourceSnapshotRepository.findBySource_IdAndCommitId(
        sourceId = dto.sourceId,
        commitId = dto.commitId,
    ).orNotFound {
        "Not found ${TestsSourceSnapshot::class.simpleName} for $dto"
    }
}
