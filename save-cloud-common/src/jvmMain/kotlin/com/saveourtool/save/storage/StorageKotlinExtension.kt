/**
 * Utils for S3 to operate with kotlin sdk
 */

package com.saveourtool.save.storage

import com.saveourtool.save.utils.getLogger
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.OctetStream
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactor.asCoroutineDispatcher
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.flux
import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.nio.ByteBuffer

/**
 * Kotlin extension for storage
 */
object StorageKotlinExtension {
    private val log: Logger = getLogger<StorageKotlinExtension>()

    /**
     * A shared http client to communicate with S3
     */
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    /**
     * A collection of headers which should be excluded since they are set by ktor
     */
    private val headersToExclude = setOf(
        HttpHeaders.ContentLength,
        HttpHeaders.ContentType,
    ).map { it.lowercase() }

    /**
     * Kotlin's async implementation
     *
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content
     */
    suspend fun <K> Storage<K>.upload(key: K, contentLength: Long, content: ByteReadChannel) {
        flux<ByteBuffer>(Schedulers.boundedElastic().asCoroutineDispatcher()) {

        }
        flow {
            content.consumeEachBufferRange { buffer, last ->
                emit(buffer)
                !last
            }
        }.asFlux(Schedulers.boundedElastic().asCoroutineDispatcher())

        val (url, signedHeaders) = generateUrlToUpload(key, contentLength)
        val response = httpClient.post {
            url(url)
            headers {
//                append(HttpHeaders.ContentLength, contentLength.toString())
//                append(HttpHeaders.ContentType, OctetStream.toString())
                appendSignedHeaders(signedHeaders)
            }
            setBody(
                object : OutgoingContent.ReadChannelContent() {
                    override val contentType = null
                    override val contentLength = null
                    override fun readFrom(): ByteReadChannel = content
                }
            )
        }
        require(response.status.isSuccess()) {
            with(response) {
                "Got response $status : ${bodyAsText()} on post to ${request.url} with headers ${request.headers}"
            }
        }
    }

    private fun HeadersBuilder.appendSignedHeaders(signedHeaders: Map<String, Collection<String>>) {
        signedHeaders.forEach { (name, values) ->
//            if (name.lowercase() !in headersToExclude) {
                appendAll(name, values)
//            }
        }
    }
}

