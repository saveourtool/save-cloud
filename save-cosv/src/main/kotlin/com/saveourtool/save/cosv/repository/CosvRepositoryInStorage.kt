package com.saveourtool.save.cosv.repository

import com.saveourtool.save.cosv.storage.CosvKey
import com.saveourtool.save.cosv.storage.CosvStorage
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.CosvMetadata
import com.saveourtool.save.entities.cosv.CosvMetadataDto
import com.saveourtool.save.utils.*

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/**
 * Implementation of [CosvRepository] using [CosvStorage]
 */
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
    ): Mono<CosvMetadataDto> = saveMetadata(entry, user, organization).flatMap { metadata ->
        cosvStorage.upload(
            metadata.toStorageKey(),
            json.encodeToString(serializer, entry).encodeToByteArray(),
        ).map { metadata }
    }

    private fun saveMetadata(
        entry: CosvSchema<*, *, *, *>,
        user: User,
        organization: Organization,
    ): Mono<CosvMetadataDto> = blockingToMono {
        val metadata = cosvMetadataRepository.findByCosvId(entry.id)
            ?.let { existedMetadata ->
                val newModified = entry.modified.toJavaLocalDateTime()
                val errorPrefix: () -> String = {
                    "Failed to upload COSV [${entry.id}/${entry.modified}]"
                }
                if (existedMetadata.modified >= newModified) {
                    throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "${errorPrefix()}: existed entry has newer version (${existedMetadata.modified})",
                    )
                }
                if (existedMetadata.user.requiredId() != user.requiredId()) {
                    throw ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "${errorPrefix()} by userId=${user.requiredId()}: already existed in save uploaded by another userId=${existedMetadata.user.requiredId()}",
                    )
                }
                if (existedMetadata.organization.requiredId() != organization.requiredId()) {
                    throw ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "${errorPrefix()} to organizationId=${organization.requiredId()}: already existed in save in another userId=${existedMetadata.organization.requiredId()}",
                    )
                }
                existedMetadata.updateBy(entry)
            }
            ?: entry.toMetadata(user, organization)
        cosvMetadataRepository.save(metadata).toDto()
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
        private fun CosvSchema<*, *, *, *>.toMetadata(
            user: User,
            organization: Organization,
        ) = CosvMetadata(
            cosvId = id,
            summary = summary ?: "Summary not provided",
            details = details ?: "Details not provided",
            severity = severity?.firstOrNull()?.score,
            severityNum = severity?.firstOrNull()?.scoreNum?.toInt() ?: 0,
            modified = modified.toJavaLocalDateTime(),
            published = (published ?: modified).toJavaLocalDateTime(),
            user = user,
            organization = organization,
        )

        private fun CosvMetadata.updateBy(entry: CosvSchema<*, *, *, *>): CosvMetadata = apply {
            summary = entry.summary ?: "Summary not provided"
            details = entry.details ?: "Details not provided"
            severity = entry.severity?.firstOrNull()?.score
            severityNum = entry.severity?.firstOrNull()
                ?.scoreNum
                ?.toInt() ?: 0
            modified = entry.modified.toJavaLocalDateTime()
            published = (entry.published ?: entry.modified).toJavaLocalDateTime()
        }

        private fun CosvMetadataDto.toStorageKey() = CosvKey(
            id = cosvId,
            modified = modified,
        )
    }
}
