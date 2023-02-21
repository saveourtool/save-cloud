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
     * @param function
     * @return result of [function] which is run using [StorageProjectReactor]
     */
    fun <T : Any> usingProjectReactor(function: StorageProjectReactor<K>.() -> T): T

    /**
     * @return [StoragePreSignedUrl] for this storage
     */
    fun usingPreSignedUrl(): StoragePreSignedUrl<K>

    /**
     * @param function
     * @return result of [function] which is run using [StoragePreSignedUrl]
     */
    fun <T : Any> usingPreSignedUrl(function: StoragePreSignedUrl<K>.() -> T): T
}
