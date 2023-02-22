package com.saveourtool.save.storage

/**
 * Base interface for Storage which implements [StorageCoroutines] and [StoragePreSignedUrl]
 *
 * @param K type of key
 */
interface StorageUsingCoroutines<K : Any> : StorageCoroutines<K>, StoragePreSignedUrl<K>
