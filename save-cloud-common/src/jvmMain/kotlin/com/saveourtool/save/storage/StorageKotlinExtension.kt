/**
 * Utils for S3 to operate with kotlin sdk
 */

package com.saveourtool.save.storage

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.OctetStream
import io.ktor.http.content.*
import io.ktor.utils.io.*

/**
 * Kotlin extension for storage
 */
object StorageKotlinExtension {
    /**
     * A shared http client to communicate with S3
     */
    private val httpClient = HttpClient()

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
        val (url, signedHeaders) = generateUrlToUpload(key, contentLength)
        httpClient.post {
            url(url)
            headers {
                appendSignedHeaders(signedHeaders)
            }
            setBody(
                object : OutgoingContent.ReadChannelContent() {
                    override val contentType: ContentType = OctetStream
                    override val contentLength = contentLength
                    override fun readFrom(): ByteReadChannel = content
                }
            )
        }
    }

    private fun HeadersBuilder.appendSignedHeaders(signedHeaders: Map<String, Collection<String>>) {
        signedHeaders.forEach { (name, values) ->
            if (name.lowercase() !in headersToExclude) {
                appendMissing(name, values)
            }
        }
    }
}

