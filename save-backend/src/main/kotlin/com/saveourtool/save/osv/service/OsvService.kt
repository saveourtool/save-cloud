package com.saveourtool.save.osv.service

import com.saveourtool.save.backend.service.vulnerability.VulnerabilityService
import com.saveourtool.save.entities.vulnerability.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.osv.storage.OsvStorage
import com.saveourtool.save.osv.utils.decodeSingleOrArrayFromStream
import com.saveourtool.save.osv.utils.decodeSingleOrArrayFromString
import com.saveourtool.save.utils.blockingMap

import com.saveourtool.osv4k.RawOsvSchema
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import java.io.InputStream

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json

/**
 * Service for vulnerabilities
 */
@Service
class OsvService(
    private val osvStorage: OsvStorage,
    private val vulnerabilityService: VulnerabilityService,
) {
    private val json = Json {
        prettyPrint = false
    }

    /**
     * Decodes [inputStream] and saves the result
     *
     * @param inputStream
     * @param authentication who uploads [inputStream]
     * @return save's vulnerability names
     */
    fun decodeAndSave(
        inputStream: InputStream,
        authentication: Authentication,
    ): Flux<String> = { json.decodeSingleOrArrayFromStream<RawOsvSchema>(inputStream) }
        .toMono()
        .flatMapIterable { it }
        .flatMap {
            save(it, authentication)
        }

    /**
     * Decodes [content] and saves the result
     *
     * @param content
     * @param authentication who uploads [content]
     * @return save's vulnerability names
     */
    fun decodeAndSave(
        content: String,
        authentication: Authentication,
    ): Flux<String> = { json.decodeSingleOrArrayFromString<RawOsvSchema>(content) }
        .toMono()
        .flatMapIterable { it }
        .flatMap {
            save(it, authentication)
        }

    /**
     * Saves [vulnerability] in S3 storage and creates a new entity in save database
     *
     * @param vulnerability
     * @param authentication who uploads [vulnerability]
     * @return save's vulnerability name
     */
    fun save(
        vulnerability: RawOsvSchema,
        authentication: Authentication,
    ): Mono<String> = osvStorage.upload(vulnerability).blockingMap {
        vulnerabilityService.save(vulnerability.toSaveVulnerabilityDto(), authentication).name
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