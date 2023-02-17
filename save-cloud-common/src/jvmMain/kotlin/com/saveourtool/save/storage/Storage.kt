package com.saveourtool.save.storage

import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer

/**
 * Base interface for Storage
 *
 * @param K type of key
 */
interface Storage<K>: StorageProjectReactor<K>, StoragePreSignedUrl<K> {
    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content as [Flow] of [ByteBuffer]
     */
    suspend fun upload(key: K, contentLength: Long, content: Flow<ByteBuffer>)

//    /**
//     * @return [StorageCoroutines] for this storage
//     */
//    fun withCoroutines(): StorageCoroutines<K>
}
