package com.saveourtool.save.cosv.service

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.cosv.processor.CosvProcessor
import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.repository.CosvSchema
import com.saveourtool.save.cosv.storage.RawCosvFileStorage
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.CosvMetadataDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.utils.*

import com.saveourtool.osv4k.*
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

import kotlinx.serialization.serializer

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
            } catch (e: Exception) {
                log.error(e) {
                    "Failed to process raw COSV file with id: $rawCosvFileId"
                }
                return@flatMap rawCosvFileStorage.markAs(listOf(rawCosvFileId), RawCosvFileStatus.FAILED)
            }
            cosvListOpt.toFlux()
                .flatMap { cosvProcessor.save(it, user, organization) }
                .collectList()
                .flatMap { rawCosvFileStorage.markAs(listOf(rawCosvFileId), RawCosvFileStatus.PROCESSED) }
        }

    /**
     * Generates COSV from [VulnerabilityDto] and saves it
     *
     * @param vulnerabilityDto as a source for COSV
     * @return [CosvMetadataDto] saved metadata
     */
    fun generateAndSave(
        vulnerabilityDto: VulnerabilityDto,
    ): Mono<CosvMetadataDto> = blockingToMono {
        val user = backendService.getUserByName(vulnerabilityDto.userInfo.name)
        val organization = vulnerabilityDto.organization?.let { backendService.getOrganizationByName(it.name) }
        user to organization
    }.flatMap { (user, organization) ->
        val osv = ManualCosvSchema(
            id = vulnerabilityDto.identifier,
            published = (vulnerabilityDto.creationDateTime ?: getCurrentLocalDateTime()).truncatedToMills(),
            modified = (vulnerabilityDto.lastUpdatedDateTime ?: getCurrentLocalDateTime()).truncatedToMills(),
            severity = listOf(
                Severity(
                    type = SeverityType.CVSS_V3,
                    score = "N/A",
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
        cosvRepository.save(
            entry = osv,
            serializer = serializer(),
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
        updater: (RawOsvSchema) -> Mono<RawOsvSchema>,
    ): Mono<CosvMetadataDto> = cosvRepository.findLatestRawExt(cosvId)
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
                    cosvRepository.save(
                        entry = newCosv.copy(modified = getCurrentLocalDateTime().truncatedToMills()),
                        serializer = serializer(),
                        user = owner,
                        organization = organization,
                    )
                }
        }

    companion object {
        private val log: Logger = getLogger<CosvService>()
    }
}
