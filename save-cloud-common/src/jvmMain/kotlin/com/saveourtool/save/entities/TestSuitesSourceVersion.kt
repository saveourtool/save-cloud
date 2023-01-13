package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import com.saveourtool.save.testsuite.TestSuitesSourceVersionDto

import java.time.LocalDateTime
import javax.persistence.*

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime

/**
 * @property snapshot [TestSuitesSourceSnapshot]
 * @property name human-readable version
 * @property createdByUser [User] created this version
 * @property creationTime time of creation this version
 * @property type
 */
@Entity
class TestSuitesSourceVersion(
    @ManyToOne
    @JoinColumn(name = "snapshot_id")
    var snapshot: TestSuitesSourceSnapshot,

    var name: String,
    @Enumerated(EnumType.STRING)
    var type: TestSuitesSourceFetchMode,

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    val createdByUser: User,
    val creationTime: LocalDateTime,
) : BaseEntityWithDtoWithId<TestSuitesSourceVersionDto>() {
    override fun toDto(): TestSuitesSourceVersionDto = TestSuitesSourceVersionDto(
        snapshotId = snapshot.requiredId(),
        name = name,
        type = type,
        createdByUserId = createdByUser.requiredId(),
        creationTime = creationTime.toKotlinLocalDateTime(),
        id = id,
    )

    companion object {
        /**
         * @param snapshotResolver returns [TestSuitesSourceSnapshot] by ID
         * @param userResolver returns [User] by ID
         * @return [TestSuitesSourceVersion] created from [TestSuitesSourceVersionDto]
         */
        fun TestSuitesSourceVersionDto.toEntity(
            snapshotResolver: (Long) -> TestSuitesSourceSnapshot,
            userResolver: (Long) -> User,
        ): TestSuitesSourceVersion = TestSuitesSourceVersion(
            snapshot = snapshotResolver(snapshotId),
            name = name,
            type = type,
            createdByUser = userResolver(createdByUserId),
            creationTime = creationTime.toJavaLocalDateTime(),
        ).apply {
            id = this@toEntity.id
        }
    }
}
