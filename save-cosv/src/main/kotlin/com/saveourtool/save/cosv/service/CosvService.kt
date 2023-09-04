package com.saveourtool.save.cosv.service

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.cosv.processor.CosvProcessorHolder
import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.utils.toJsonArrayOrSingle
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.RawCosvExt
import com.saveourtool.save.entities.vulnerability.*
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
    private val cosvProcessorHolder: CosvProcessorHolder,
) {
    private val json = Json {
        prettyPrint = false
    }

    /**
     * Decodes [inputStreams] and saves the result
     *
     * @param sourceId
     * @param inputStreams
     * @param authentication who uploads [inputStream]
     * @param organizationName to which is uploaded
     * @return save's vulnerability identifiers
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun decodeAndSave(
        sourceId: String,
        inputStreams: Flux<InputStream>,
        authentication: Authentication,
        organizationName: String,
    ): Flux<String> {
        val user = backendService.getUserByName(authentication.name)
        val organization = backendService.getOrganizationByName(organizationName)
        return inputStreams.flatMap { inputStream ->
            decode(sourceId, json.decodeFromStream<JsonElement>(inputStream), user, organization)
        }.save(user)
    }

    /**
     * Decodes [content] and saves the result
     *
     * @param sourceId
     * @param content
     * @param authentication who uploads [content]
     * @param organizationName to which is uploaded
     * @return save's vulnerability identifiers
     */
    fun decodeAndSave(
        sourceId: String,
        content: String,
        authentication: Authentication,
        organizationName: String,
    ): Flux<String> {
        val user = backendService.getUserByName(authentication.name)
        val organization = backendService.getOrganizationByName(organizationName)
        return decode(sourceId, json.parseToJsonElement(content), user, organization).save(user)
    }

    /**
     * Saves OSVs from [jsonElement] in COSV repository (S3 storage)
     *
     * @param sourceId
     * @param jsonElement
     * @param user who uploads content
     * @param organization to which is uploaded
     * @return save's vulnerability
     */
    private fun decode(
        sourceId: String,
        jsonElement: JsonElement,
        user: User,
        organization: Organization,
    ): Flux<VulnerabilityDto> = jsonElement.toMono()
        .flatMapIterable { it.toJsonArrayOrSingle() }
        .flatMap { cosvProcessorHolder.process(sourceId, it.jsonObject, user, organization) }

    /**
     * Creates entities in save database
     *
     * @receiver save's vulnerability
     * @param user [user] that uploads who uploads
     * @return save's vulnerability identifiers
     */
    private fun Flux<VulnerabilityDto>.save(
        user: User,
    ): Flux<String> = collectList()
        .blockingMap { vulnerabilities ->
            vulnerabilities.map { backendService.saveVulnerability(it, user).identifier }
        }
        .flatMapIterable { it }

    /**
     * Finds COSV with validating save database
     *
     * @param cosvId [VulnerabilityDto.identifier]
     * @return found COSV
     */
    fun findById(
        cosvId: String,
    ): Mono<RawOsvSchema> = blockingToMono {
        backendService.findVulnerabilityByName(cosvId)
    }
        .switchIfEmptyToNotFound {
            "Not found vulnerability $cosvId in save database"
        }
        .flatMap { vulnerability ->
            cosvRepository.findLatestById(vulnerability.identifier, serializer<RawOsvSchema>())
        }

    /**
     * Finds extended COSV
     *
     * @param cosvId [RawOsvSchema.id]
     * @return found [RawCosvExt]
     */
    fun findExtById(
        cosvId: String,
    ): Mono<RawCosvExt> = cosvRepository.findLatestRawExt(cosvId)

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
    ): Flux<RawCosvExt> = cosvRepository.findRawExtByFilter(
        if (isOwner) {
            authentication?.let { filter.copy(authorName = it.name) } ?: filter
        } else {
            filter
        }
    )

    /**
     * @param cosvId
     * @param status
     * @return found [RawCosvExt]
     */
    fun getByCosvIdAndStatus(
        cosvId: String,
        status: VulnerabilityStatus,
    ): Mono<RawCosvExt> = cosvRepository.findLatestRawExtByCosvIdAndStatus(cosvId, status)
}
