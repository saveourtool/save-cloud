package com.saveourtool.save.cosv.repository

import com.saveourtool.save.cosv.storage.CosvKey
import com.saveourtool.save.cosv.storage.CosvStorage
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.CosvMetadata
import com.saveourtool.save.entities.cosv.CosvMetadataDto
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.collectToInputStream
import com.saveourtool.save.utils.upload
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CosvRepositoryInStorage(
    private val cosvStorage: CosvStorage,
    private val cosvMetadataRepository: CosvMetadataRepository,
) : CosvRepository {
    private val json = Json {
        prettyPrint = false
    }

    override fun <D, A_E, A_D, A_R_D> save(
        entry: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
        user: User,
        organization: Organization
    ): Mono<CosvMetadataDto> = blockingToMono {
        cosvMetadataRepository.save(
            CosvMetadata(
                cosvId = entry.id,
                summary = entry.summary ?: "Summary not provided",
                details = entry.details ?: "Details not provided",
                severity = entry.severity?.firstOrNull()?.score,
                severityNum = entry.severity?.firstOrNull()?.scoreNum?.toInt() ?: 0,
                user = user,
                organization = organization,
            )
        ).toDto()
    }.flatMap { metadata ->
        cosvStorage.upload(
            metadata.toStorageKey(),
            json.encodeToString(serializer, entry).encodeToByteArray(),
        ).map { metadata }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <D, A_E, A_D, A_R_D> findLatestById(
        id: String,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>
    ): CosvSchemaMono<D, A_E, A_D, A_R_D> = blockingToMono { cosvMetadataRepository.findByCosvId(id)?.toDto() }
        .flatMap { metadata ->
            cosvStorage.download(metadata.toStorageKey())
                .collectToInputStream()
                .map { content -> json.decodeFromStream(serializer, content) }
        }

    companion object {
        private fun CosvMetadataDto.toStorageKey() = CosvKey(
            id = cosvId,
            modified = updateDate,
        )
    }
}