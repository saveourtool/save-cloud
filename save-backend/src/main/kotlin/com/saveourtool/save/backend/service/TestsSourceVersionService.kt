package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestsSourceSnapshotRepository
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.test.TestsSourceVersionInfo

import org.springframework.stereotype.Service

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Service for [TestsSourceVersionInfo]
 */
@Service
class TestsSourceVersionService(
    private val snapshotRepository: TestsSourceSnapshotRepository,
) {
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
    ): TestsSourceSnapshotDto? = snapshotRepository.findBySource_Organization_NameAndSource_NameAndCommitId(organizationName, sourceName, version)
        ?.toDto()

    /**
     * @param organizationName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun getAllAsInfo(
        organizationName: String,
    ): List<TestsSourceVersionInfo> = snapshotRepository.findAllBySource_Organization_Name(organizationName)
        .map { it.toVersionInfo() }

    /**
     * @param organizationName
     * @param sourceName
     * @return list of [TestsSourceVersionInfo] found by provided values
     */
    fun getAllAsInfo(
        organizationName: String,
        sourceName: String,
    ): List<TestsSourceVersionInfo> = snapshotRepository.findAllBySource_Organization_NameAndSource_Name(organizationName, sourceName)
        .map { it.toVersionInfo() }

    /**
     * @param organizationName
     * @param sourceName
     * @return all fetched version of [TestsSourceSnapshot] found by provided values
     */
    fun getAllVersions(
        organizationName: String,
        sourceName: String,
    ): List<String> = snapshotRepository.findAllBySource_Organization_NameAndSource_Name(organizationName, sourceName)
        .map(TestsSourceSnapshot::commitId)

    companion object {
        private fun TestsSourceSnapshot.toVersionInfo() = TestsSourceVersionInfo(
            organizationName = source.organization.name,
            sourceName = source.name,
            commitId = commitId,
            commitTime = commitTime.toKotlinLocalDateTime(),
            version = commitId,
            creationTime = commitTime.toKotlinLocalDateTime(),
        )
    }
}
