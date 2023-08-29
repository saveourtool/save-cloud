package com.saveourtool.save.cosv.service

import com.saveourtool.save.backend.service.IVulnerabilityService
import com.saveourtool.save.cosv.processor.CosvProcessorHolder
import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.utils.toJsonArrayOrSingle
import com.saveourtool.save.entities.vulnerability.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.*

import com.saveourtool.osv4k.OsvSchema
import com.saveourtool.osv4k.RawOsvSchema
import com.saveourtool.osv4k.Reference
import com.saveourtool.osv4k.ReferenceType
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import java.io.InputStream

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

typealias EmptyCosvSchema = OsvSchema<Unit, Unit, Unit, Unit>

/**
 * Service for vulnerabilities
 */
@Service
class CosvService(
    private val cosvRepository: CosvRepository,
    private val vulnerabilityService: IVulnerabilityService,
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
     * @return save's vulnerability names
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun decodeAndSave(
        sourceId: String,
        inputStreams: Flux<InputStream>,
        authentication: Authentication,
    ): Flux<String> = inputStreams.flatMap { inputStream ->
        decode(sourceId, json.decodeFromStream<JsonElement>(inputStream))
    }.save(authentication)

    /**
     * Decodes [content] and saves the result
     *
     * @param sourceId
     * @param content
     * @param authentication who uploads [content]
     * @return save's vulnerability names
     */
    fun decodeAndSave(
        sourceId: String,
        content: String,
        authentication: Authentication,
    ): Flux<String> = decode(sourceId, json.parseToJsonElement(content)).save(authentication)

    /**
     * Saves OSVs from [jsonElement] in COSV repository (S3 storage)
     *
     * @param sourceId
     * @param jsonElement
     * @return save's vulnerability
     */
    private fun decode(
        sourceId: String,
        jsonElement: JsonElement,
    ): Flux<VulnerabilityDto> = jsonElement.toMono()
        .flatMapIterable { it.toJsonArrayOrSingle() }
        .flatMap { cosvProcessorHolder.process(sourceId, it.jsonObject) }

    /**
     * Creates entities in save database
     *
     * @receiver save's vulnerability
     * @param authentication who uploads
     * @return save's vulnerability names
     */
    private fun Flux<VulnerabilityDto>.save(
        authentication: Authentication,
    ): Flux<String> = collectList()
        .blockingMap { vulnerabilities ->
            vulnerabilities.map { vulnerabilityService.save(it, authentication).name }
        }
        .flatMapIterable { it }

    /**
     * Finds OSV with validating save database
     *
     * @param id [VulnerabilityDto.name]
     * @return found OSV
     */
    fun findById(
        id: String,
    ): Mono<RawOsvSchema> = blockingToMono {
        vulnerabilityService.findByName(id)
    }
        .switchIfEmptyToNotFound {
            "Not found vulnerability $id in save database"
        }
        .flatMap { vulnerability ->
            cosvRepository.findLatestById(vulnerability.name, serializer<RawOsvSchema>())
        }

    /**
     * @param proposeSaveOsvRequest
     * @param creatorUserId
     * @return save's vulnerability names
     */
    fun createNew(
        proposeSaveOsvRequest: ProposeSaveOsvRequest,
        creatorUserId: Long,
    ): Mono<String> = blockingToMono { vulnerabilityService.save(proposeSaveOsvRequest, creatorUserId) }
        .flatMap { vulnerabilityMeta ->
            val osv = EmptyCosvSchema(
                schemaVersion = "1.5.0",
                id = vulnerabilityMeta.name,
                modified = vulnerabilityMeta.createDate.orNotFound {
                    "CreationDate is not provided on vulnerability meta ${vulnerabilityMeta.name}"
                }.toKotlinLocalDateTime(),
                summary = proposeSaveOsvRequest.shortDescription,
                details = proposeSaveOsvRequest.description,
                aliases = proposeSaveOsvRequest.vulnerabilityIdentifier?.let { listOf(it) },
                references = proposeSaveOsvRequest.relatedLink?.let { listOf(Reference(ReferenceType.WEB, it)) }
            )
            cosvRepository.save(osv, serializer<EmptyCosvSchema>())
                .map { osv.id }
        }

    companion object {
        private fun RawOsvSchema.toSaveVulnerabilityDto(): VulnerabilityDto = VulnerabilityDto(
            name = "TO_GENERATE",  // will be generated on saving to database
            vulnerabilityIdentifier = id,
            progress = 0,
            projects = emptyList(),
            description = details,
            shortDescription = summary.orEmpty(),
            relatedLink = null,
            language = VulnerabilityLanguage.OTHER,
            userInfo = UserInfo(name = ""),  // will be set on saving to database
            organization = null,
            dates = buildList {
                add(modified.asVulnerabilityDateDto(VulnerabilityDateType.CVE_UPDATED))
                published?.asVulnerabilityDateDto(VulnerabilityDateType.INTRODUCED)?.run { add(this) }
                withdrawn?.asVulnerabilityDateDto(VulnerabilityDateType.FIXED)?.run { add(this) }
            },
            participants = emptyList(),
            status = VulnerabilityStatus.CREATED,
            tags = setOf("osv-schema")
        )

        private fun LocalDateTime.asVulnerabilityDateDto(type: VulnerabilityDateType) = VulnerabilityDateDto(
            date = this,
            type = type,
            vulnerabilityName = "NOT_USED_ON_SAVE",
        )
    }
}
