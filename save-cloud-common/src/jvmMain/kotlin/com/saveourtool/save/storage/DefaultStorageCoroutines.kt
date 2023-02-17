package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.key.Metastore
import com.saveourtool.save.storage.key.S3KeyAdapter
import com.saveourtool.save.utils.isNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

import reactor.kotlin.core.publisher.toFlux

import java.nio.ByteBuffer
import java.time.Instant

import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.asPublisher
import kotlinx.coroutines.withContext
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response
import software.amazon.awssdk.services.s3.model.S3Exception
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

/**
 * S3 implementation of [StorageCoroutines]
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param metastore [Metastore] metastore with S3 keys
 * @param K type of key
 */
class DefaultStorageCoroutines<K : Any>(
    private val s3Operations: S3Operations,
    private val metastore: Metastore<K>,
) : StorageCoroutines<K> {
    override suspend fun list(): Flow<K> = flow {
        var lastResponse = doListObjectV2()
        emitKeys(lastResponse)
        while (lastResponse.isTruncated) {
            lastResponse = doListObjectV2(lastResponse.nextContinuationToken())
            emitKeys(lastResponse)
        }
    }

    private suspend fun doListObjectV2(continuationToken: String? = null): ListObjectsV2Response =
            s3Operations.listObjectsV2(metastore.commonPrefix, continuationToken).asDeferred().await()

    private suspend fun FlowCollector<K>.emitKeys(response: ListObjectsV2Response) {
        response.contents()
            .forEach { s3Object ->
                val key = metastore.buildKey(s3Object.key())
                // TODO: need to log that found a key in storage, which doesn't exist in metastore
                key?.let { emit(it) }
            }
    }

    override suspend fun doesExist(key: K): Boolean = buildExistedS3Key(key)?.let { s3Key ->
        s3Operations.headObject(s3Key)
            .asDeferred()
            .await()
    }.isNotNull()

    override suspend fun contentLength(key: K): Long? = buildExistedS3Key(key)?.let { s3Key ->
        s3Operations.headObject(s3Key)
            .asDeferred()
            .await()
            ?.contentLength()
    }

    override suspend fun lastModified(key: K): Instant? = buildExistedS3Key(key)?.let { s3Key ->
        s3Operations.headObject(s3Key)
            .asDeferred()
            .await()
            ?.lastModified()
    }

    override suspend fun delete(key: K): Boolean {
        return buildExistedS3Key(key)?.let { s3Key ->
            s3Operations.deleteObject(s3Key)
                .asDeferred()
                .await()
                .isNotNull()
                .also {
                    cleanup(key)
                }
        } == true
    }

    override suspend fun upload(key: K, content: Flow<ByteBuffer>): Long {
        val s3Key = buildNewS3Key(key)
        try {
            val uploadResponse = s3Operations.createMultipartUpload(s3Key)
                .asDeferred()
                .await()
            val partCounter = AtomicLong()
            val completedParts = content.map { buffer ->
                s3Operations.uploadPart(
                    uploadResponse,
                    partCounter.incrementAndGet(),
                    AsyncRequestBody.fromByteBuffer(buffer)
                ).asDeferred().await()
            }.toList()
            s3Operations.completeMultipartUpload(uploadResponse, completedParts)
            return requireNotNull(contentLength(key)) {
                "Cannot find contentLength for uploaded key $key"
            }
        } catch (ex: S3Exception) {
            cleanup(key)
            throw ex
        }
    }

    override suspend fun upload(key: K, contentLength: Long, content: Flow<ByteBuffer>) {
        val s3Key = buildNewS3Key(key)
        try {
            s3Operations.putObject(
                s3Key,
                contentLength,
                AsyncRequestBody.fromPublisher(content.asPublisher(s3Operations.coroutineDispatcher)),
            )
        } catch (ex: Exception) {
            cleanup(key)
            throw ex
        }
    }

    override suspend fun download(key: K): Flow<ByteBuffer> = buildExistedS3Key(key)?.let {
        s3Operations.getObject(it)
            .thenApply { getResponse ->
                getResponse?.toFlux()?.asFlow() ?: emptyFlow()
            }
            .asDeferred()
            .await()
    } ?: emptyFlow()

    override suspend fun move(source: K, target: K): Boolean {
        return buildExistedS3Key(source)?.let { sourceS3Key ->
            val targetS3Key = buildNewS3Key(target)
            s3Operations.copyObject(sourceS3Key, targetS3Key)
                .thenCompose { copyResponse ->
                    copyResponse?.let {
                        s3Operations.deleteObject(sourceS3Key)
                            .thenApply { it.isNotNull() }
                    } ?: CompletableFuture.completedFuture(false)
                }
                .asDeferred()
                .await()
        } ?: false
    }

    private suspend fun cleanup(key: K) {
        if (metastore.isDatabaseUnderlying) {
            withContext(Dispatchers.IO) {
                metastore.delete(key)
            }
        } else {
            metastore.delete(key)
        }
    }

    private suspend fun buildExistedS3Key(key: K): String? = if (metastore.isDatabaseUnderlying) {
        withContext(Dispatchers.IO) {
            metastore.buildExistedS3Key(key)
        }
    } else {
        metastore.buildExistedS3Key(key)
    }

    private suspend fun buildNewS3Key(key: K): String = if (metastore.isDatabaseUnderlying) {
        withContext(Dispatchers.IO) {
            metastore.buildNewS3Key(key)
        }
    } else {
        metastore.buildNewS3Key(key)
    }
}
