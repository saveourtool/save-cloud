package com.saveourtool.save.osv.storage

import com.saveourtool.save.osv.processor.AnyOsvSchema
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.storage.AbstractSimpleReactiveStorage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToParts
import com.saveourtool.save.utils.upload

import com.saveourtool.osv4k.OsvSchema
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.extra.math.max

import java.nio.ByteBuffer
import java.util.Comparator

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Storage for Vulnerabilities.
 *
 * For now, we store vulnerabilities in S3 with `id` as a key.
 * We can migrate to NoSql database in the future.
 */
@Service
class OsvStorage(
    s3OperationsPropertiesProvider: S3OperationsProperties.Provider,
    s3Operations: S3Operations,
) : AbstractSimpleReactiveStorage<OsvKey>(
    s3Operations,
    concatS3Key(s3OperationsPropertiesProvider.s3Storage.prefix, "vulnerabilities"),
) {
    private val json = Json {
        prettyPrint = false
    }

    override fun doBuildKeyFromSuffix(s3KeySuffix: String): OsvKey {
        val (id, modified) = s3KeySuffix.s3KeyToParts()
        return OsvKey(id, LocalDateTime.parse(modified))
    }

    override fun doBuildS3KeySuffix(key: OsvKey): String = concatS3Key(key.id, key.modified.toString())

    /**
     * @param vulnerability
     * @param serializer
     * @return ID
     */
    fun <S : AnyOsvSchema> upload(vulnerability: S, serializer: KSerializer<S>): Mono<String> = upload(
        vulnerability.toStorageKey(),
        json.encodeToString(serializer, vulnerability).encodeToByteArray(),
    ).map { vulnerability.id }

    /**
     * @param id [OsvKey.id]
     * @return content for storage key with provided id and max [OsvKey.modified]
     */
    fun downloadLatest(id: String): Flux<ByteBuffer> = list(id)
        .max(Comparator.comparing(OsvKey::modified))
        .flatMapMany { download(it) }

    companion object {
        private fun OsvSchema<*, *, *, *>.toStorageKey() = OsvKey(
            id = id,
            modified = modified,
        )
    }
}
