package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceSnapshot.Companion.toEntity
import com.saveourtool.save.storage.AbstractStorageWithDatabase
import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.utils.getByIdOrNotFound

import org.springframework.stereotype.Component

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
}
