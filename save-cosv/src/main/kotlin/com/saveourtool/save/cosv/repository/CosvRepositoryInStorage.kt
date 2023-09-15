package com.saveourtool.save.cosv.repository

import com.saveourtool.save.cosv.storage.CosvFileStorage
import com.saveourtool.save.entities.cosv.CosvFile
import com.saveourtool.save.utils.*

import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/**
 * Implementation of [CosvRepository] using [CosvFileStorage]
 */
@Component
class CosvRepositoryInStorage(
    private val cosvFileStorage: CosvFileStorage,
) : CosvRepository {
    private val json = Json {
        prettyPrint = false
    }

    override fun <D, A_E, A_D, A_R_D> save(
        content: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): Mono<CosvFile> = cosvFileStorage.upload(
        content.toCosvFile(),
        json.encodeToString(serializer, content).encodeToByteArray(),
    )

    @OptIn(ExperimentalSerializationApi::class)
    override fun <D, A_E, A_D, A_R_D> download(
        key: CosvFile,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>
    ): CosvSchemaMono<D, A_E, A_D, A_R_D> = cosvFileStorage.download(key)
        .collectToInputStream()
        .map { content -> json.decodeFromStream(serializer, content) }

    override fun deleteAll(identifier: String): Flux<LocalDateTime> = cosvFileStorage.list(identifier)
        .flatMap { key -> cosvFileStorage.delete(key).map { key.modified.toKotlinLocalDateTime() } }

    companion object {
        private fun CosvSchema<*, *, *, *>.toCosvFile() = CosvFile(
            identifier = id,
            modified = modified.toJavaLocalDateTime(),
        )
    }
}
