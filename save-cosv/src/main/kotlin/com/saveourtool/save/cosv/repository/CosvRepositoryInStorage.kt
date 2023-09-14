package com.saveourtool.save.cosv.repository

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.VulnerabilityExt
import com.saveourtool.save.entities.cosv.VulnerabilityMetadata
import com.saveourtool.save.entities.cosv.VulnerabilityMetadataDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityLanguage
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.utils.*

import com.saveourtool.osv4k.RawOsvSchema
import com.saveourtool.save.cosv.storage.CosvFileStorage
import com.saveourtool.save.entities.cosv.CosvFile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer

/**
 * Implementation of [CosvRepository] using [CosvFileStorage]
 */
@Component
class CosvRepositoryInStorage(
    private val cosvFileStorage: CosvFileStorage,
    private val vulnerabilityMetadataRepository: VulnerabilityMetadataRepository,
    private val lnkVulnerabilityMetadataTagRepository: LnkVulnerabilityMetadataTagRepository,
    private val backendService: IBackendService,
) : CosvRepository {
    private val json = Json {
        prettyPrint = false
    }

    override fun <D, A_E, A_D, A_R_D> save(
        entry: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
        user: User,
        organization: Organization?,
    ): Mono<VulnerabilityMetadataDto> = saveMetadata(entry, user, organization).flatMap { metadata ->
        cosvFileStorage.upload(
            metadata.toStorageKey(),
            json.encodeToString(serializer, entry).encodeToByteArray(),
        ).map { metadata }
    }

    private fun saveMetadata(
        entry: CosvSchema<*, *, *, *>,
        user: User,
        organization: Organization?,
    ): Mono<VulnerabilityMetadataDto> = blockingToMono {
        val metadata = vulnerabilityMetadataRepository.findByIdentifier(entry.id)
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
                        "${errorPrefix()} by userId=${user.requiredId()}: " +
                                "already existed in save uploaded by another userId=${existedMetadata.user.requiredId()}",
                    )
                }
                existedMetadata.organization?.run {
                    if (requiredId() != organization?.requiredId()) {
                        throw ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "${errorPrefix()} to organizationId=${requiredId()}: " +
                                    "already existed in save in another organizationId=${existedMetadata.organization?.requiredId()}",
                        )
                    }
                }
                existedMetadata.updateBy(entry)
            }
            ?: entry.toMetadata(user, organization)
        vulnerabilityMetadataRepository.save(metadata).toDto()
    }

    override fun <D, A_E, A_D, A_R_D> findLatestById(
        cosvId: String,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>
    ): CosvSchemaMono<D, A_E, A_D, A_R_D> = blockingToMono { vulnerabilityMetadataRepository.findByIdentifier(cosvId) }
        .flatMap { doDownload(it, serializer) }

    override fun findLatestRawExt(cosvId: String): Mono<VulnerabilityExt> = blockingToMono { vulnerabilityMetadataRepository.findByIdentifier(cosvId) }
        .flatMap { it.toRawCosvExt() }

    private fun VulnerabilityMetadata.toRawCosvExt() = doDownload(this, serializer<RawOsvSchema>())
        .blockingMap { content ->
            VulnerabilityExt(
                metadata = toDto(),
                cosv = content,
                saveContributors = content.getSaveContributes().map { backendService.getUserByName(it.name).toUserInfo() },
                tags = lnkVulnerabilityMetadataTagRepository.findByVulnerabilityMetadataId(requiredId()).map { it.tag.name }.toSet(),
            )
        }

    @OptIn(ExperimentalSerializationApi::class)
    private fun <D, A_E, A_D, A_R_D> doDownload(
        metadata: VulnerabilityMetadata,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ) = cosvFileStorage.download(metadata.toDto().toStorageKey())
        .collectToInputStream()
        .map { content -> json.decodeFromStream(serializer, content) }

    override fun delete(cosvId: String): Flux<LocalDateTime> = blockingToMono {
        vulnerabilityMetadataRepository.findByIdentifier(cosvId)?.let {
            vulnerabilityMetadataRepository.delete(it)
        }
    }.flatMapMany {
        cosvFileStorage.list(cosvId)
            .flatMap { key -> cosvFileStorage.delete(key).map { key.modified.toKotlinLocalDateTime() } }
    }

    companion object {
        private fun CosvSchema<*, *, *, *>.toMetadata(
            user: User,
            organization: Organization?,
        ) = VulnerabilityMetadata(
            identifier = id,
            summary = summary ?: "Summary not provided",
            details = details ?: "Details not provided",
            severityNum = severity?.firstOrNull()?.scoreNum?.toFloat() ?: 0f,
            modified = modified.toJavaLocalDateTime(),
            submitted = getCurrentLocalDateTime().toJavaLocalDateTime(),
            user = user,
            organization = organization,
            language = getLanguage() ?: VulnerabilityLanguage.OTHER,
            status = VulnerabilityStatus.PENDING_REVIEW,
        )

        private fun VulnerabilityMetadata.updateBy(entry: CosvSchema<*, *, *, *>): VulnerabilityMetadata = apply {
            summary = entry.summary ?: "Summary not provided"
            details = entry.details ?: "Details not provided"
            severityNum = entry.severity?.firstOrNull()
                ?.scoreNum
                ?.toFloat() ?: 0f
            modified = entry.modified.toJavaLocalDateTime()
        }

        private fun VulnerabilityMetadataDto.toStorageKey() = CosvFile(
            identifier = identifier,
            modified = modified.toJavaLocalDateTime(),
        )
    }
}
