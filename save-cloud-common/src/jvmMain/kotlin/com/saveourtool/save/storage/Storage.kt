package com.saveourtool.save.storage

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant

typealias UrlWithHeaders = Pair<URL, Map<String, Collection<String>>>

/**
 * Base interface for Storage
 *
 * @param K type of key
 */
interface Storage<K> {
    /**
     * @return list of keys in storage
     */
    fun list(): Flux<K>

    /**
     * @param key a key to be checked
     * @return true if the key exists, otherwise false
     */
    fun doesExist(key: K): Mono<Boolean>

    /**
     * @param key a key to be checked
     * @return content size in bytes
     */
    fun contentLength(key: K): Mono<Long>

    /**
     * @param key a key to be checked
     * @return when a key was modified last time
     */
    fun lastModified(key: K): Mono<Instant>

    /**
     * @param key a key to be deleted
     * @return true if the object deleted, otherwise false
     */
    fun delete(key: K): Mono<Boolean>

    /**
     * @param key a key for provided content
     * @param content
     * @return count of written bytes
     */
    fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long>

    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content
     * @return [Mono] without value
     */
    fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit>

    /**
     * @param key a key for provided content
     * @param content
     * @return count of written bytes
     */
    fun overwrite(key: K, content: Flux<ByteBuffer>): Mono<Long> = delete(key)
        .flatMap { upload(key, content) }

    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content
     * @return [Mono] without value
     */
    fun overwrite(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit> = delete(key)
        .flatMap { upload(key, contentLength, content) }

    /**
     * @param key a key to download content
     * @return downloaded content
     */
    fun download(key: K): Flux<ByteBuffer>

    /**
     * @param key a key to download content
     * @return URL to download content
     */
    fun generateUrlToDownload(key: K): URL

    /**
     * @param key a key to download content
     * @param contentLength a content length of content
     * @return URL with headers to upload content
     */
    fun generateUrlToUpload(key: K, contentLength: Long): UrlWithHeaders

    /**
     * @param source a key of source
     * @param target a key of target
     * @return true if the [source] deleted, otherwise false
     */
    fun move(source: K, target: K): Mono<Boolean>
}
