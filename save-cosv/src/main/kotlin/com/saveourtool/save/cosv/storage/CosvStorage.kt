package com.saveourtool.save.cosv.storage

import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.repository.CosvSchemaKSerializer
import com.saveourtool.save.cosv.repository.CosvSchemaMono
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.storage.AbstractSimpleReactiveStorage
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.s3KeyToParts
import com.saveourtool.save.utils.collectToInputStream
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.upload

import com.saveourtool.osv4k.OsvSchema as CosvSchema
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.extra.math.max

import java.util.Comparator

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/**
 * Storage for Vulnerabilities.
 *
 * For now, we store vulnerabilities in S3 with `id` as a key.
 * We can migrate to NoSql database in the future.
 */
@Service
class CosvStorage(
    s3OperationsPropertiesProvider: S3OperationsProperties.Provider,
    s3Operations: S3Operations,
) : CosvRepository, AbstractSimpleReactiveStorage<CosvKey>(
    s3Operations,
    concatS3Key(s3OperationsPropertiesProvider.s3Storage.prefix, "vulnerabilities"),
) {
    private val json = Json {
        prettyPrint = false
    }

    override fun doBuildKeyFromSuffix(s3KeySuffix: String): CosvKey {
        val (id, modified) = s3KeySuffix.s3KeyToParts()
        return CosvKey(id, LocalDateTime.parse(modified))
    }

    override fun doBuildS3KeySuffix(key: CosvKey): String = concatS3Key(key.id, key.modified.toString())

    override fun <D, A_E, A_D, A_R_D> save(
        entry: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>
    ): Mono<Unit> = upload(
        entry.toStorageKey(),
        json.encodeToString(serializer, entry).encodeToByteArray(),
    ).map {
        log.debug {
            "Uploaded ${entry.id} to storage"
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <D, A_E, A_D, A_R_D> findLatestById(
        id: String,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): CosvSchemaMono<D, A_E, A_D, A_R_D> = list(id)
        .max(Comparator.comparing(CosvKey::modified))
        .flatMap { key ->
            download(key)
                .collectToInputStream()
                .map { content -> json.decodeFromStream(serializer, content) }
        }

    companion object {
        private val log: Logger = getLogger<CosvStorage>()

        private fun CosvSchema<*, *, *, *>.toStorageKey() = CosvKey(
            id = id,
            modified = modified,
        )
    }
}
