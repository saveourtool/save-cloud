package com.saveourtool.save.storage

import java.net.URL

typealias UrlWithHeaders = Pair<URL, Map<String, Collection<String>>>

/**
 * Base interface for Storage with methods for [pre-signed url](https://docs.aws.amazon.com/AmazonS3/latest/userguide/example_s3_Scenario_PresignedUrl_section.html)
 *
 * @param K type of key
 */
interface StoragePreSignedUrl<K : Any> {
    /**
     * @param key a key to download content
     * @return URL to download content if [key] valid, otherwise -- null
     */
    fun generateUrlToDownload(key: K): URL?

    /**
     * @param key a key to download content
     * @param contentLength a content length of content
     * @return URL with headers to upload content
     */
    fun generateUrlToUpload(key: K, contentLength: Long): UrlWithHeaders
}
