package com.saveourtool.save.storage.request

import java.net.URL

/**
 * @param key key to upload
 * @param url pre signed url to upload [key]
 * @param headers required headers to upload [key] using [url]
 * @param urlFromContainer pre signed url to upload [key]
 * @param headersFromContainer required headers to upload [key] using [urlFromContainer]
 */
data class UploadRequest<K : Any>(
    val key: K,
    val url: URL,
    val headers: Map<String, List<String>>,
    val urlFromContainer: URL,
    val headersFromContainer: Map<String, List<String>>,
)
