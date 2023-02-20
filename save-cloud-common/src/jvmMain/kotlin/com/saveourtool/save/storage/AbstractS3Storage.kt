package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.AbstractS3KeyDatabaseManager
import com.saveourtool.save.storage.key.S3KeyManager
import com.saveourtool.save.utils.*
import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * S3 implementation of Storage
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param prefix a common prefix for all S3 keys in this storage
 * @param K type of key
 */
abstract class AbstractS3Storage<K : Any>(
    private val s3Operations: S3Operations,
    private val s3KeyManager: S3KeyManager<K>,
) : Storage<K> {
    private val log: Logger = getLogger(this::class)

    override fun list(): Flux<K> = s3Operations.listObjectsV2(s3KeyManager.commonPrefix)
        .flatMapIterable { response ->
            response.contents().mapNotNull {
                val s3Key = it.key()
                val key = s3KeyManager.findKey(s3Key)
                if (!key.isNotNull()) {
                    log.warn {
                        "Found s3 key $s3Key which is not valid by ${s3KeyManager::class.simpleName}"
                    }
                }
                key
            }
        }

    override fun download(key: K): Flux<ByteBuffer> = findExistedS3Key(key)
        .flatMap { s3Key ->
        s3Operations.getObject(s3Key)
    }
        .flatMapMany {
        it.toFlux()
    }

    override fun generateUrlToDownload(key: K): URL = findExistedS3Key(key)
        .map { s3Key ->
            s3Operations.requestToDownloadObject(s3Key, downloadDuration)
                .also { request ->
                    require(request.isBrowserExecutable) {
                        "Pre-singer url to download object should be browser executable (header-less)"
                    }
                }
                .url()
        }


    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> =
            s3Operations.uploadObject(buildS3Key(key), content)
                .flatMap {
                    contentLength(key)
                }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit> =
        createNewS3Key(key)
            .flatMap { s3Key ->
                s3Operations.uploadObject(s3Key, contentLength, content)
            }
            .map { response ->
                log.debug { "Uploaded $key with versionId: ${response.versionId()}" }
            }

    override fun move(source: K, target: K): Mono<Boolean> =
        findExistedS3Key(source)
            .zipWith(createNewS3Key(target))
            .flatMap { (sourceS3Key, targetS3Key) ->
                s3Operations.copyObject(sourceS3Key, targetS3Key)
            }
                .flatMap {
                    delete(source)
                }

    override fun delete(key: K): Mono<Boolean> = findExistedS3Key(key).flatMap { s3Key ->
        s3Operations.deleteObject(s3Key)
    }
        .thenReturn(true)
        .defaultIfEmpty(false)

    override fun lastModified(key: K): Mono<Instant> = findExistedS3Key(key).flatMap { s3Key ->
        s3Operations.headObject(s3Key)
    }
            .map { response ->
                response.lastModified()
            }

    override fun contentLength(key: K): Mono<Long> = findExistedS3Key(key).flatMap { s3Key ->
        s3Operations.headObject(s3Key)
    }
            .map { response ->
                response.contentLength()
            }

    override fun doesExist(key: K): Mono<Boolean> = findExistedS3Key(key).flatMap { s3Key ->
        s3Operations.headObject(s3Key)
    }
            .map { true }
            .defaultIfEmpty(false)

    private fun deleteKey(key: K): Mono<Unit> = if (s3KeyManager is AbstractS3KeyDatabaseManager<*, *, *>) {
        blockingToMono {
            s3KeyManager.delete(key)
        }
    } else {
        Mono.fromCallable {
            s3KeyManager.delete(key)
        }
    }

    private fun findExistedS3Key(key: K): Mono<String> = if (s3KeyManager is AbstractS3KeyDatabaseManager<*, *, *>) {
        blockingToMono {
            s3KeyManager.findExistedS3Key(key)
        }
    } else {
        s3KeyManager.findExistedS3Key(key).toMono()
    }

    private fun createNewS3Key(key: K): Mono<String> = if (s3KeyManager is AbstractS3KeyDatabaseManager<*, *, *>) {
        blockingToMono {
            s3KeyManager.createNewS3Key(key)
        }
    } else {
        s3KeyManager.createNewS3Key(key).toMono()
    }

    companion object {
        private val downloadDuration = 15.minutes
        private fun String.validateSuffix(): String = also { suffix ->
            require(!suffix.startsWith(PATH_DELIMITER)) {
                "Suffix cannot start with $PATH_DELIMITER: $suffix"
            }
        }
    }
}
