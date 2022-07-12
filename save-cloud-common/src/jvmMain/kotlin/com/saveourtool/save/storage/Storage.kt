package com.saveourtool.save.storage

import com.saveourtool.save.domain.ProjectCoordinates
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

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
     * @param key a ket to be checked
     * @return content size in bytes
     */
    fun contentSize(key: K): Mono<Long>

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
     * @param key a key to download content
     * @return downloaded content
     */
    fun download(key: K): Flux<ByteBuffer>

    /**
     * Extensions which expects [ProjectCoordinates] as part of key
     *
     * @param K type of inner key (without [ProjectCoordinates])
     */
    interface WithProjectCoordinates<K> : Storage<WithProjectCoordinates.Key<K>> {
        /**
         * @param projectCoordinates
         * @return list of keys in storage
         */
        fun list(projectCoordinates: ProjectCoordinates?): Flux<K> = list()
            .filter { it.projectCoordinates == projectCoordinates }
            .map { it.key }

        /**
         * @param projectCoordinates
         * @param key a key to be checked
         * @return true if the key exists, otherwise false
         */
        fun exists(projectCoordinates: ProjectCoordinates?, key: K): Mono<Boolean> =
                doesExist(Key(projectCoordinates, key))

        /**
         * @param projectCoordinates
         * @param key a ket to be checked
         * @return content size in bytes
         */
        fun contentSize(projectCoordinates: ProjectCoordinates?, key: K): Mono<Long> =
                contentSize(Key(projectCoordinates, key))

        /**
         * @param projectCoordinates
         * @param key a key to be deleted
         * @return true if the object deleted, otherwise false
         */
        fun delete(projectCoordinates: ProjectCoordinates?, key: K): Mono<Boolean> =
                delete(Key(projectCoordinates, key))

        /**
         * @param projectCoordinates
         * @param key a key for provided content
         * @param content
         * @return count of written bytes
         */
        fun upload(projectCoordinates: ProjectCoordinates?, key: K, content: Flux<ByteBuffer>): Mono<Long> =
                upload(Key(projectCoordinates, key), content)

        /**
         * @param projectCoordinates
         * @param key a key to download content
         * @return downloaded content
         */
        fun download(projectCoordinates: ProjectCoordinates?, key: K): Flux<ByteBuffer> =
                download(Key(projectCoordinates, key))

        /**
         * @property projectCoordinates
         * @property key
         */
        class Key<K> internal constructor(
            val projectCoordinates: ProjectCoordinates?,
            val key: K
        )
    }
}
