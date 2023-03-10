package com.saveourtool.save.storage

import java.net.URL

/**
 * @param key key to upload
 * @param url pre signed url to upload [key]
 * @param urlFromContainer pre signed url to upload [key]
 * @param headers required headers to upload [key] using [url]
 */
data class UploadRequest<K : Any>(
    val key: K,
    val url: URL,
    val urlFromContainer: URL,
    val headers: Map<String, List<String>>,
)
