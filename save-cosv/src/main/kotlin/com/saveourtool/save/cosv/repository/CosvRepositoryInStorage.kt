package com.saveourtool.save.cosv.repository

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.cosv.processor.CosvProcessor
import com.saveourtool.save.cosv.service.VulnerabilityMetadataService
import com.saveourtool.save.cosv.storage.CosvFileStorage
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.CosvFile
import com.saveourtool.save.entities.cosv.VulnerabilityExt
import com.saveourtool.save.entities.cosv.VulnerabilityMetadata
import com.saveourtool.save.entities.cosv.VulnerabilityMetadataDto
import com.saveourtool.save.utils.*

import org.slf4j.Logger
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
    private val vulnerabilityMetadataRepository: VulnerabilityMetadataRepository,
    private val lnkVulnerabilityMetadataTagRepository: LnkVulnerabilityMetadataTagRepository,
    private val backendService: IBackendService,
    private val vulnerabilityMetadataService: VulnerabilityMetadataService,
) : CosvRepository {
    private val json = Json {
        prettyPrint = false
    }

    override fun <D, A_E, A_D, A_R_D> save(
        entry: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
        user: User,
        organization: Organization?,
    ): Mono<VulnerabilityMetadataDto> = cosvFileStorage.upload(
        entry.toCosvFile(),
        json.encodeToString(serializer, entry).encodeToByteArray(),
    ).flatMap { cosvFile ->
        blockingToMono {
            vulnerabilityMetadataService.createOrUpdate(
                cosvFile,
                entry,
                user,
                organization,
            )
        }
            .onErrorResume { error ->
                log.error(error) {
                    "Failed to save metadata for $cosvFile, cleaning up"
                }
                cosvFileStorage.delete(cosvFile)
                    .then(Mono.error(error))
            }
            .map { it.toDto() }
    }

    override fun findLatestExt(identifier: String): Mono<VulnerabilityExt> = blockingToMono { vulnerabilityMetadataRepository.findByIdentifier(identifier) }
        .flatMap { it.toVulnerabilityExt() }

    private fun VulnerabilityMetadata.toVulnerabilityExt() = doDownload(this, CosvProcessor.rawSerializer)
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
    ) = cosvFileStorage.download(metadata.requiredLatestCosvFile())
        .collectToInputStream()
        .map { content -> json.decodeFromStream(serializer, content) }

    override fun deleteAll(identifier: String): Flux<LocalDateTime> = cosvFileStorage.list(identifier)
        .flatMap { key -> cosvFileStorage.delete(key).map { key.modified.toKotlinLocalDateTime() } }

    companion object {
        private val log: Logger = getLogger<CosvRepositoryInStorage>()

        private fun CosvSchema<*, *, *, *>.toCosvFile() = CosvFile(
            identifier = id,
            modified = modified.toJavaLocalDateTime(),
        )
    }
}
