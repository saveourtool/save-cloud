package com.saveourtool.save.osv.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.AbstractSimpleReactiveStorage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.utils.upload

import com.saveourtool.osv4k.RawOsvSchema
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Storage for Vulnerabilities.
 *
 * For now, we store vulnerabilities in S3 with `id` as a key.
 * We can migrate to NoSql database in the future.
 */
@Service
class OsvStorage(
    configProperties: ConfigProperties,
    s3Operations: S3Operations,
) : AbstractSimpleReactiveStorage<String>(
    s3Operations,
    concatS3Key(configProperties.s3Storage.prefix, "vulnerabilities"),
) {
    private val json = Json {
        prettyPrint = false
    }

    override fun doBuildKeyFromSuffix(s3KeySuffix: String): String = s3KeySuffix

    override fun doBuildS3KeySuffix(key: String): String = key

    /**
     * @param vulnerability
     */
    fun upload(vulnerability: RawOsvSchema): Mono<String> = upload(
        vulnerability.id,
        json.encodeToString(vulnerability).encodeToByteArray(),
    ).map { vulnerability.id }

    /**
     * @param content
     * @return uploaded vulnerability.id
     */
    fun uploadSingle(content: String): Mono<String> {
        val vulnerability: RawOsvSchema = Json.decodeFromString(content)
        return upload(
            vulnerability.id,
            content.encodeToByteArray(),
        ).map { vulnerability.id }
    }

    /**
     * @param content
     * @return uploaded content length
     */
    fun uploadBatch(content: String): Mono<Long> {
        val vulnerability: RawOsvSchema = Json.decodeFromString(content)
        return upload(
            vulnerability.id,
            content.encodeToByteArray(),
        )
    }
}
