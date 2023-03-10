package com.saveourtool.save.storage

import com.saveourtool.save.utils.orNotFound
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.net.URL

/**
 * Base interface for Storage which implements [StorageProjectReactor] and [StoragePreSignedUrl]
 *
 * @param K type of key
 */
interface ReactiveStorage<K : Any> : StorageProjectReactor<K>, StoragePreSignedUrl<K> {
    /**
     * @param key
     * @return generated [URL] to download provided [key] [K]
     * @throws ResponseStatusException with status [HttpStatus.NOT_FOUND]
     */
    fun generateRequiredUrlToDownload(key: K): URL = generateUrlToDownload(key)
        .orNotFound {
            "Not found $key in ${this::class.simpleName} storage"
        }
}
