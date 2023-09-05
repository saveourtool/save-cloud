package com.saveourtool.save.cosv.service

import com.saveourtool.osv4k.*
import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.cosv.processor.CosvProcessor
import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.utils.toJsonArrayOrSingle
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.vulnerability.*
import com.saveourtool.save.entities.vulnerability.VulnerabilityExt
import com.saveourtool.save.filters.VulnerabilityFilter
import com.saveourtool.save.utils.*

import com.saveourtool.save.cosv.repository.CosvSchema
import com.saveourtool.save.entities.cosv.CosvMetadataDto
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import java.io.InputStream

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

/**
 * Service for vulnerabilities
 */
@Service
class CosvService(
    private val cosvRepository: CosvRepository,
    private val backendService: IBackendService,
    private val cosvProcessor: CosvProcessor,
) {
    private val json = Json {
        prettyPrint = false
    }

    /**
     * Decodes [inputStreams] and saves the result
     *
     * @param inputStreams
     * @param authentication who uploads [inputStream]
     * @param organizationName to which is uploaded
     * @return vulnerability identifiers
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun decodeAndSave(
        inputStreams: Flux<InputStream>,
        authentication: Authentication,
        organizationName: String,
    ): Flux<String> {
        val user = backendService.getUserByName(authentication.name)
        val organization = backendService.getOrganizationByName(organizationName)
        return inputStreams.flatMap { inputStream ->
            decode(json.decodeFromStream<JsonElement>(inputStream), user, organization)
        }
    }

    /**
     * Decodes [content] and saves the result
     *
     * @param content
     * @param authentication who uploads [content]
     * @param organizationName to which is uploaded
     * @return vulnerability identifiers
     */
    fun decodeAndSave(
        content: String,
        authentication: Authentication,
        organizationName: String,
    ): Flux<String> {
        val user = backendService.getUserByName(authentication.name)
        val organization = backendService.getOrganizationByName(organizationName)
        return decode(json.parseToJsonElement(content), user, organization)
    }

    /**
     * Saves OSVs from [jsonElement] in COSV repository (S3 storage)
     *
     * @param jsonElement
     * @param user who uploads content
     * @param organization to which is uploaded
     * @return vulnerability identifier
     */
    private fun decode(
        jsonElement: JsonElement,
        user: User,
        organization: Organization,
    ): Flux<String> = jsonElement.toMono()
        .flatMapIterable { it.toJsonArrayOrSingle() }
        .flatMap { cosvProcessor.process(it.jsonObject, user, organization) }
        .map { it.cosvId }

    /**
     * Finds COSV with validating save database
     *
     * @param cosvId [VulnerabilityDto.identifier]
     * @return found COSV
     */
    fun findById(
        cosvId: String,
    ): Mono<RawOsvSchema> = cosvRepository.findLatestById(cosvId, serializer<RawOsvSchema>())

    /**
     * Finds extended COSV
     *
     * @param cosvId [RawOsvSchema.id]
     * @return found [VulnerabilityExt]
     */
    fun findExtById(
        cosvId: String,
    ): Mono<VulnerabilityExt> = cosvRepository.findLatestRawExt(cosvId)

    /**
     * Finds all extended vulnerabilities
     *
     * @param userName
     * @return all found [VulnerabilityExt]
     */
    fun findExtByUser(
        userName: String,
    ): Flux<VulnerabilityExt> = cosvRepository.findAllLatestRawExtByUserName(userName)

    /**
     * @param filter filter for COSV
     * @param isOwner
     * @param authentication [Authentication] describing an authenticated request
     * @return list of OSV with that match [filter]
     */
    fun getByFilter(
        filter: VulnerabilityFilter,
        isOwner: Boolean,
        authentication: Authentication?,
    ): Flux<VulnerabilityExt> = cosvRepository.findRawExtByFilter(
        if (isOwner) {
            authentication?.let { filter.copy(authorName = it.name) } ?: filter
        } else {
            filter
        }
    )

    /**
     * @param cosvId
     * @param status
     * @return found [VulnerabilityExt]
     */
    fun getByCosvIdAndStatus(
        cosvId: String,
        status: VulnerabilityStatus,
    ): Mono<VulnerabilityExt> = cosvRepository.findLatestRawExtByCosvIdAndStatus(cosvId, status)

    fun save(
        vulnerabilityDto: VulnerabilityDto,
    ): Mono<CosvMetadataDto> = blockingToMono {
        val user = backendService.getUserByName(vulnerabilityDto.userInfo.name)
        val organization = vulnerabilityDto.organization?.let { backendService.getOrganizationByName(it.name) }
        user to organization
    }.flatMap { (user, organization) ->
        val osv = CosvSchema<Unit, Unit, Unit, Unit>(
            id = vulnerabilityDto.identifier,
            published = vulnerabilityDto.creationDateTime ?: getCurrentLocalDateTime(),
            modified = vulnerabilityDto.lastUpdatedDateTime ?: getCurrentLocalDateTime(),
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
            credits = vulnerabilityDto.participants.asCredits().takeUnless { it.isEmpty() },
        )
        cosvRepository.save(
            entry = osv,
            serializer = serializer(),
            user = user,
            organization = organization,
        )
    }
}
