package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.test.TestsSourceSnapshotInfo
import com.saveourtool.save.test.TestsSourceVersionDto
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode

import java.time.LocalDateTime
import javax.persistence.*

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property snapshot [TestsSourceSnapshot]
 * @property name human-readable version
 * @property creationTime time of creation this version
 */
@Entity
class TestsSourceVersion(
    @ManyToOne
    @JoinColumn(name = "snapshot_id")
    var snapshot: TestsSourceSnapshot,

    var name: String,
    val creationTime: LocalDateTime,
) : BaseEntityWithDtoWithId<TestsSourceVersionDto>() {
    override fun toDto(): TestsSourceVersionDto = TestsSourceVersionDto(
        snapshotId = snapshot.requiredId(),
        name = name,
        creationTime = creationTime.toKotlinLocalDateTime(),
        id = id,
    )

    /**
     * @return [TestsSourceVersionInfo] created from [TestsSourceVersion]
     */
    fun toInfo(): TestsSourceVersionInfo = TestsSourceVersionInfo(
        snapshotInfo = TestsSourceSnapshotInfo(
            organizationName = snapshot.source.organization.name,
            sourceName = snapshot.source.name,
            commitId = snapshot.commitId,
            commitTime = snapshot.commitTime.toKotlinLocalDateTime(),
        ),
        version = name,
        creationTime = creationTime.toKotlinLocalDateTime(),
    )

    companion object {
        /**
         * @param snapshotResolver returns [TestsSourceSnapshot] by ID
         * @return [TestsSourceVersion] created from [TestsSourceVersionDto]
         */
        fun TestsSourceVersionDto.toEntity(
            snapshotResolver: (Long) -> TestsSourceSnapshot,
        ): TestsSourceVersion = TestsSourceVersion(
            snapshot = snapshotResolver(snapshotId),
            name = name,
            creationTime = creationTime.toJavaLocalDateTime(),
        ).apply {
            id = this@toEntity.id
        }
    }
}
