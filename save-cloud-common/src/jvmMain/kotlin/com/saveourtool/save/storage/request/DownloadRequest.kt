package com.saveourtool.save.storage.request

import java.net.URL

/**
 * @param key key to download
 * @param url pre signed url to download [key]
 * @param urlFromContainer pre signed url to download [key]
 */
data class DownloadRequest<K : Any>(
    val key: K,
    val url: URL,
    val urlFromContainer: URL,
)
