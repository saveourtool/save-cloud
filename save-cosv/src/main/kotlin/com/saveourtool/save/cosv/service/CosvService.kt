package com.saveourtool.save.cosv.service

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.cosv.processor.CosvProcessor
import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.repository.CosvSchema
import com.saveourtool.save.cosv.storage.RawCosvFileStorage
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.entities.cosv.VulnerabilityMetadataDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.utils.*

import com.saveourtool.osv4k.*
import com.saveourtool.save.cosv.repository.LnkVulnerabilityMetadataTagRepository
import com.saveourtool.osv4k.RawOsvSchema as RawCosvSchema
import com.saveourtool.save.entities.cosv.VulnerabilityExt
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import org.springframework.expression.spel.ast.Identifier

private typealias ManualCosvSchema = CosvSchema<Unit, Unit, Unit, Unit>

/**
 * Service for vulnerabilities
 */
@Service
class CosvService(
    private val rawCosvFileStorage: RawCosvFileStorage,
    private val cosvRepository: CosvRepository,
    private val backendService: IBackendService,
    private val cosvProcessor: CosvProcessor,
    private val vulnerabilityMetadataService: VulnerabilityMetadataService,
    private val lnkVulnerabilityMetadataTagRepository: LnkVulnerabilityMetadataTagRepository,
) {
    /**
     * @param rawCosvFileIds
     * @return empty [Mono]
     */
    fun process(
        rawCosvFileIds: Collection<Long>,
    ): Mono<Unit> = rawCosvFileIds.toFlux()
        .flatMap { rawCosvFileId ->
            rawCosvFileStorage.getOrganizationAndOwner(rawCosvFileId)
                .flatMap { (organization, user) ->
                    doProcess(rawCosvFileId, user, organization)
                }
        }
        .collectList()
        .map {
            log.debug {
                "Finished processing raw COSV files $rawCosvFileIds"
            }
        }

    private fun doProcess(
        rawCosvFileId: Long,
        user: User,
        organization: Organization,
    ): Mono<Unit> = rawCosvFileStorage.downloadById(rawCosvFileId)
        .collectToInputStream()
        .flatMap { inputStream ->
            val cosvListOpt = try {
                cosvProcessor.decode(inputStream)
            } catch (e: SerializationException) {
                val errorMessage: () -> String = { "Failed to process raw COSV file with id: $rawCosvFileId" }
                log.error(e, errorMessage)
                return@flatMap rawCosvFileStorage.update(rawCosvFileId, RawCosvFileStatus.FAILED, "$errorMessage is due to ${e.message}")
            }
            cosvListOpt.toFlux()
                .flatMap { cosvProcessor.save(it, user, organization) }
                .collectList()
                .flatMap { rawCosvFileStorage.update(rawCosvFileId, RawCosvFileStatus.PROCESSED, "Processed as ${it.map(VulnerabilityMetadataDto::identifier)}") }
        }

    /**
     * Generates COSV from [VulnerabilityDto] and saves it
     *
     * @param vulnerabilityDto as a source for COSV
     * @return [VulnerabilityMetadataDto] saved metadata
     */
    fun generateAndSave(
        vulnerabilityDto: VulnerabilityDto,
    ): Mono<VulnerabilityMetadataDto> = blockingToMono {
        val user = backendService.getUserByName(vulnerabilityDto.userInfo.name)
        val organization = vulnerabilityDto.organization?.let { backendService.getOrganizationByName(it.name) }
        user to organization
    }.flatMap { (user, organization) ->
        val generatedCosv = ManualCosvSchema(
            id = vulnerabilityDto.identifier,
            published = (vulnerabilityDto.creationDateTime ?: getCurrentLocalDateTime()).truncatedToMills(),
            modified = (vulnerabilityDto.lastUpdatedDateTime ?: getCurrentLocalDateTime()).truncatedToMills(),
            severity = listOf(
                Severity(
                    type = SeverityType.CVSS_V3,
                    score = vulnerabilityDto.severity,
                    scoreNum = vulnerabilityDto.progress.toString(),
                )
            ),
            summary = vulnerabilityDto.shortDescription,
            details = vulnerabilityDto.description,
            references = vulnerabilityDto.relatedLink?.let { relatedLink ->
                listOf(
                    Reference(
                        type = ReferenceType.WEB,
                        url = relatedLink,
                    )
                )
            },
            credits = vulnerabilityDto.getAllParticipants().asCredits().takeUnless { it.isEmpty() },
        )
        save(
            cosv = generatedCosv,
            user = user,
            organization = organization,
        )
    }

    /**
     * @param cosvId
     * @param updater
     * @return [Mono] with new metadata
     */
    fun update(
        cosvId: String,
        updater: (RawCosvSchema) -> Mono<RawCosvSchema>,
    ): Mono<VulnerabilityMetadataDto> = getVulnerabilityExt(cosvId)
        .blockingMap { rawCosvExt ->
            rawCosvExt to Pair(
                backendService.getUserByName(rawCosvExt.metadata.user.name),
                rawCosvExt.metadata.organization?.let { organization ->
                    backendService.getOrganizationByName(organization.name)
                }
            )
        }
        .flatMap { (rawCosvExt, infoFromDatabase) ->
            val (owner, organization) = infoFromDatabase
            updater(rawCosvExt.cosv)
                .flatMap { newCosv ->
                    save(
                        cosv = newCosv.copy(modified = getCurrentLocalDateTime().truncatedToMills()),
                        user = owner,
                        organization = organization,
                    )
                }
        }

    private inline fun <reified D, reified A_E, reified A_D, reified A_R_D> save(
        cosv: CosvSchema<D, A_E, A_D, A_R_D>,
        user: User,
        organization: Organization?,
    ): Mono<VulnerabilityMetadataDto> = cosvRepository.save(cosv, serializer())
        .flatMap { key ->
            blockingToMono {
                vulnerabilityMetadataService.createOrUpdate(key, cosv, user, organization).toDto()
            }
                .onErrorResume { error ->
                    log.error(error) {
                        "Failed to save/update metadata for $key, cleaning up"
                    }
                    cosvRepository.delete(key).then(Mono.error(error))
                }
        }

    /**
     * Finds extended raw cosv with [CosvSchema.id] and max [CosvSchema.modified]
     *
     * @param identifier
     * @return [Mono] with [VulnerabilityExt]
     */
    fun getVulnerabilityExt(identifier: String): Mono<VulnerabilityExt> = blockingToMono { vulnerabilityMetadataService.findByIdentifier(identifier) }
        .flatMap { metadata ->
            cosvRepository.download(metadata.requiredLatestCosvFile(), serializer<RawCosvSchema>()).blockingMap { content ->
                VulnerabilityExt(
                    metadata = metadata.toDto(),
                    cosv = content,
                    saveContributors = content.getSaveContributes().map { backendService.getUserByName(it.name).toUserInfo() },
                    tags = lnkVulnerabilityMetadataTagRepository.findByVulnerabilityMetadataId(metadata.requiredId()).map { it.tag.name }.toSet(),
                )
            }
        }


    companion object {
        private val log: Logger = getLogger<CosvService>()
    }
}
