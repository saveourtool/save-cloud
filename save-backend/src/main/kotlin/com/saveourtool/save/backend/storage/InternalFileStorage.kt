package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.AbstractS3Storage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToPartsTill
import com.saveourtool.save.utils.*
import org.slf4j.Logger
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct

/**
 * Storage for internal files used by backend: save-cli and save-agent
 */
@Component
class InternalFileStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractS3Storage<InternalFileKey>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "internal-storage")
) {
    private val webClientToGithub = WebClient.builder().build()
    private val isInitialized = AtomicBoolean(false)

    /**
     * An init method to upload internal files to S3 from classpath or github
     */
    @PostConstruct
    fun init() {
        Flux.concat(
            overwriteSaveAgentFromClasspath(),
             downloadIfMissedSaveCliFromClasspath(),
//            downloadIfMissedSaveCliFromGithub(),
        )
            .collectList()
            .map {
                isInitialized.set(true)
            }
            .subscribe()
    }

    override fun generateUrlToDownload(key: InternalFileKey): URL {
        if (isInitialized.get()) {
            return super.generateUrlToDownload(key)
        } else {
            throw IllegalStateException("${InternalFileStorage::class.simpleName} not initialized yet")
        }
    }

    override fun buildKey(s3KeySuffix: String): InternalFileKey {
        val (version, name) = s3KeySuffix.s3KeyToPartsTill(prefix)
        return InternalFileKey(
            name = name,
            version = version,
        )
    }

    override fun buildS3KeySuffix(key: InternalFileKey): String = concatS3Key(key.version, key.name)

    private fun overwriteSaveAgentFromClasspath(): Mono<Unit> {
        val key = InternalFileKey.forSaveAgent
        return downloadFromClasspath(key.name) {
            "Can't find ${key.name}"
        }
            .flatMap { resource ->
                overwrite(
                    key,
                    resource.contentLength(),
                    resource.toByteBufferFlux(),
                )
            }
    }

    private fun downloadIfMissedSaveCliFromClasspath(): Mono<Unit> {
        val key = InternalFileKey.forSaveCli()
        return doesExist(key)
            .filter { it.not() }
            .flatMap {
                downloadFromClasspath(key.name) {
                    "Can't find ${key.name}"
                }
                    .flatMap { resource ->
                        upload(
                            key,
                            resource.contentLength(),
                            resource.toByteBufferFlux(),
                        )
                    }
            }
            .defaultIfEmpty(Unit)
    }

    private fun downloadIfMissedSaveCliFromGithub(): Mono<Unit> {
        val key = InternalFileKey.forSaveCli()
        return doesExist(key)
            .filter { it.not() }
            .flatMap {
                val uri = "https://github.com/saveourtool/save-cli/releases/download/v${key.version}/${key.name}"
                webClientToGithub.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .exchangeToMono { response ->
                        val content: Flux<ByteBuffer> = response.bodyToFlux()
                        val contentLength = response.headers().contentLength()
                        if (contentLength.isPresent) {
                            upload(key, contentLength.asLong, content)
                        } else {
                            upload(key, content)
                                .map { uploadedContentLength ->
                                    log.debug {
                                        "Downloaded $uploadedContentLength bytes from $uri as $key to storage"
                                    }
                                }
                        }
                    }
            }
            .defaultIfEmpty(Unit)
    }

    companion object {
        private val log: Logger = getLogger<InternalFileKey>()
    }
}
