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

object StorageKotlinExtension {
    /**
     * A shared http client to communicate with S3
     */
    internal val httpClient = HttpClient()
}

/**
 * Kotlin's async implementation
 *
 * @param key a key for provided content
 * @param contentLength a content length of content
 * @param content
 */
suspend fun <K> Storage<K>.upload(key: K, contentLength: Long, content: ByteReadChannel) {
    StorageKotlinExtension.httpClient.post {
        url(generateUrlToUpload(key, contentLength))
        setBody(
            object : OutgoingContent.ReadChannelContent() {
                override val contentType: ContentType = OctetStream
                override val contentLength = contentLength
                override fun readFrom(): ByteReadChannel = content
            }
        )
    }
}
