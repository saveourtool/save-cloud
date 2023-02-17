package com.saveourtool.save.storage

/**
 * Base interface for Storage
 *
 * @param K type of key
 */
interface Storage<K>: StoragePreSignedUrl<K> {
    /**
     * @return [StorageCoroutines] for this storage
     */
    fun withCoroutines(): StorageCoroutines<K>

    /**
     * @return [StorageProjectReactor] for this storage
     */
    fun withProjectReactor(): StorageProjectReactor<K>
}
