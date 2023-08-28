package com.saveourtool.save.cosv.service

import com.saveourtool.save.backend.service.IVulnerabilityService
import com.saveourtool.save.cosv.processor.CosvProcessorHolder
import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.utils.toJsonArrayOrSingle
import com.saveourtool.save.entities.vulnerability.*
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
}
