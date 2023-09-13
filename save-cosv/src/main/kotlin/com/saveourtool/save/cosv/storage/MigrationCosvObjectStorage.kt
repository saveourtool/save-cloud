package com.saveourtool.save.cosv.storage

import com.saveourtool.save.entities.cosv.CosvFile
import com.saveourtool.save.storage.AbstractMigrationReactiveStorage
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.springframework.stereotype.Component

/**
 * Migration to new storage
 */
@Component
class MigrationCosvObjectStorage(
    oldStorage: CosvStorage,
    newStorage: CosvFileStorage,
) : AbstractMigrationReactiveStorage<CosvKey, CosvFile>(
    newStorage, oldStorage, newStorage
) {
    override fun CosvKey.toNewKey(): CosvFile = CosvFile(
        identifier = id,
        modified = modified.toJavaLocalDateTime(),
    )

    override fun CosvFile.toOldKey(): CosvKey = CosvKey(
        id = identifier,
        modified = modified.toKotlinLocalDateTime(),
    )
}