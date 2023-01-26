package com.saveourtool.save.storage

/**
 * Abstract storage which has an init method to migrate keys from old storage to new one
 */
abstract class AbstractMigrationStorage<K : Any>(
    oldStorage: Storage<K>,
    newStorage: Storage<K>,
) : AbstractMigrationKeysAndStorage<K, K>(oldStorage, newStorage) {
    override fun K.toNewKey(): K = this
    override fun K.toOldKey(): K = this
}
