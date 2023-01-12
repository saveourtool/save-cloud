package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotDto
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property source [TestSuitesSource]
 * @property commitId hash-code
 * @property commitTime time of commit
 */
@Entity
class TestSuitesSourceSnapshot(
    @ManyToOne
    @JoinColumn(name = "source_id")
    var source: TestSuitesSource,

    var commitId: String,
    var commitTime: LocalDateTime,
): BaseEntityWithDtoWithId<TestSuitesSourceSnapshotDto>() {
    override fun toDto(): TestSuitesSourceSnapshotDto = TestSuitesSourceSnapshotDto(
        sourceId = source.requiredId(),
        commitId = commitId,
        commitTime = commitTime.toKotlinLocalDateTime(),
        id = id,
    )

    companion object {
        /**
         * @param sourceResolver returns [TestSuitesSource] by ID
         * @return [TestSuitesSourceSnapshot] created from [TestSuitesSourceSnapshotDto]
         */
        fun TestSuitesSourceSnapshotDto.toEntity(sourceResolver: (Long) -> TestSuitesSource): TestSuitesSourceSnapshot = TestSuitesSourceSnapshot(
            source = sourceResolver(sourceId),
            commitId = commitId,
            commitTime = commitTime.toJavaLocalDateTime(),
        ).apply {
            this.id  = this@toEntity.id
        }
    }
}
