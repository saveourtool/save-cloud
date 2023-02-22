package com.saveourtool.save.storage

/**
 * Base interface for Storage which implements [StorageProjectReactor] and [StoragePreSignedUrl]
 *
 * @param K type of key
 */
interface StorageUsingProjectReactor<K : Any> : StorageProjectReactor<K>, StoragePreSignedUrl<K>
