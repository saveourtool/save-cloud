package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import software.amazon.awssdk.awscore.presigner.PresignedRequest
import software.amazon.awssdk.http.SdkHttpMethod
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct
import kotlin.time.Duration.Companion.minutes

/**
 * S3 implementation of Storage
 *
 * @param s3Operations [S3Operations] to operate with S3
 * @param prefix a common prefix for all S3 keys in this storage
 * @param K type of key
 */
abstract class AbstractS3Storage<K>(
    private val s3Operations: S3Operations,
    prefix: String,
) : Storage<K> {
    private val log: Logger = getLogger(this::class)

    /**
     * A common prefix endings with [PATH_DELIMITER] for all s3 keys in this storage
     */
    protected val prefix: String = prefix.removeSuffix(PATH_DELIMITER) + PATH_DELIMITER

    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitStarted = AtomicBoolean(false)

    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitFinished = AtomicBoolean(false)

    /**
     * Init method
     */
    @PostConstruct
    fun init() {
        require(!isInitStarted.compareAndExchange(false, true)) {
            "Init method cannot be called more than 1 time, initialization is in progress"
        }
        doInitAsync()
            .doOnSuccess {
                require(!isInitFinished.compareAndExchange(false, true)) {
                    "Init method cannot be called more than 1 time. Initialization already finished by another project"
                }
            }
            .subscribe()
    }

    /**
     * Async init method
     *
     * @return [Mono] without body
     */
    protected open fun doInitAsync(): Mono<Unit> = Mono.just(Unit)

    private fun <R> validateAndRun(action: () -> R): R {
        require(isInitFinished.get()) {
            "Any method of ${javaClass.simpleName} should be called after init method is finished"
        }
        return action()
    }

    override fun list(): Flux<K> = validateAndRun {
        s3Operations.listObjectsV2(prefix)
            .flatMapIterable { response ->
                response.contents().map {
                    buildKey(it.key().removePrefix(prefix))
                }
            }
    }

    override fun download(key: K): Flux<ByteBuffer> = validateAndRun {
        s3Operations.getObject(buildS3Key(key))
            .flatMapMany {
                it.toFlux()
            }
    }

    override fun generateUrlToDownload(key: K): URL = validateAndRun {
        s3Operations.requestToDownloadObject(buildS3Key(key), presignedDuration)
            .also { request ->
                require(request.isValid(SdkHttpMethod.GET)) {
                    "Pre-singer url to upload object should be header-less"
                }
            }
            .url()
    }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> = validateAndRun {
        s3Operations.uploadObject(buildS3Key(key), content)
            .flatMap {
                contentLength(key)
            }
    }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit> = validateAndRun {
        s3Operations.uploadObject(buildS3Key(key), contentLength, content)
            .map { response ->
                log.debug { "Uploaded $key with versionId: ${response.versionId()}" }
            }
    }

    override fun generateUrlToUpload(key: K, contentLength: Long): URL = validateAndRun {
        s3Operations.requestToUploadObject(buildS3Key(key), contentLength, presignedDuration)
            .also { request ->
                require(request.isValid(SdkHttpMethod.POST)) {
                    "Pre-singer url to download object should be browser executable (header-less)"
                }
            }
            .url()
    }

    override fun move(source: K, target: K): Mono<Boolean> = validateAndRun {
        s3Operations.copyObject(buildS3Key(source), buildS3Key(target))
            .flatMap {
                delete(source)
            }
    }

    override fun delete(key: K): Mono<Boolean> = validateAndRun {
        s3Operations.deleteObject(buildS3Key(key))
            .thenReturn(true)
            .defaultIfEmpty(false)
    }

    override fun lastModified(key: K): Mono<Instant> = validateAndRun {
        s3Operations.headObject(buildS3Key(key))
            .map { response ->
                response.lastModified()
            }
    }

    override fun contentLength(key: K): Mono<Long> = validateAndRun {
        s3Operations.headObject(buildS3Key(key))
            .map { response ->
                response.contentLength()
            }
    }

    override fun doesExist(key: K): Mono<Boolean> = validateAndRun {
        s3Operations.headObject(buildS3Key(key))
            .map { true }
            .defaultIfEmpty(false)
    }

    /**
     * @param s3KeySuffix cannot start with [PATH_DELIMITER]
     * @return [K] is built from [s3KeySuffix]
     */
    protected abstract fun buildKey(s3KeySuffix: String): K

    /**
     * @param key
     * @return suffix for s3 key, cannot start with [PATH_DELIMITER]
     */
    protected abstract fun buildS3KeySuffix(key: K): String

    private fun buildS3Key(key: K) = prefix + buildS3KeySuffix(key).validateSuffix()

    companion object {
        private val presignedDuration = 15.minutes
        private fun String.validateSuffix(): String = also { suffix ->
            require(!suffix.startsWith(PATH_DELIMITER)) {
                "Suffix cannot start with $PATH_DELIMITER: $suffix"
            }
        }

        // copied from software/amazon/awssdk/services/s3/internal/signing/DefaultS3Presigner.java:505
        // and remove checking GET
        private fun PresignedRequest.isValid(httpMethod: SdkHttpMethod): Boolean =
                httpRequest().method() == httpMethod &&
                        signedPayload() == null &&
                        (signedHeaders().isEmpty() || signedHeaders().size == 1 && signedHeaders().containsKey("host"))
    }
}
