package com.saveourtool.save.storage

/**
 * Base interface for Storage
 *
 * @param K type of key
 */
interface Storage<K : Any> {
    /**
     * @return [StorageProjectReactor] for this storage
     */
    fun usingProjectReactor(): StorageProjectReactor<K>

    /**
     * @return [StoragePreSignedUrl] for this storage
     */
    fun usingPreSignedUrl(): StoragePreSignedUrl<K>
}
