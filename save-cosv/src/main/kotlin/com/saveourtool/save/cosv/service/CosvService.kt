package com.saveourtool.save.cosv.service

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

import com.saveourtool.osv4k.RawOsvSchema
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
     * @param sourceId
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
}
