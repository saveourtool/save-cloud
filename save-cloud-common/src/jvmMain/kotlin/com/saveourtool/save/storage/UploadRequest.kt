package com.saveourtool.save.storage

import java.net.URI
import java.net.URL

/**
 * @param key key to upload
 * @param url pre signed url to upload [key]
 * @param uri pre signed uri to upload [key]
 * @param headers required headers to upload [key] using [url]
 */
data class UploadRequest<K : Any>(
    val key: K,
    val url: URL,
    val uri: URI,
    val headers: Map<String, List<String>>,
)
