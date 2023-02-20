package com.saveourtool.save.storage

import java.net.URL

/**
 * Base interface for Storage with methods for [pre-signed url](https://docs.aws.amazon.com/AmazonS3/latest/userguide/example_s3_Scenario_PresignedUrl_section.html)
 *
 * @param K type of key
 */
interface StoragePreSignedUrl<K> {
    /**
     * @param key a key to download content
     * @return URL to download content
     */
    fun generateUrlToDownload(key: K): URL
}
