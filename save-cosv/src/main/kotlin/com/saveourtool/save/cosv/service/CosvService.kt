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
     * Decodes [inputStream] and saves the result
     *
     * @param sourceId
     * @param inputStream
     * @param authentication who uploads [inputStream]
     * @return save's vulnerability names
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun decodeAndSave(
        sourceId: String,
        inputStream: InputStream,
        authentication: Authentication,
    ): Flux<String> = decodeAndSave(sourceId, json.decodeFromStream<JsonElement>(inputStream), authentication)

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
    ): Flux<String> = decodeAndSave(sourceId, json.parseToJsonElement(content), authentication)

    /**
     * Saves OSVs from [jsonElement] in S3 storage and creates entities in save database
     *
     * @param sourceId
     * @param jsonElement
     * @param authentication who uploads OSV
     * @return save's vulnerability names
     */
    private fun decodeAndSave(
        sourceId: String,
        jsonElement: JsonElement,
        authentication: Authentication,
    ): Flux<String> = jsonElement.toMono()
        .flatMapIterable { it.toJsonArrayOrSingle() }
        .flatMap { cosvProcessorHolder.process(sourceId, it.jsonObject) }
        .blockingMap {
            vulnerabilityService.save(it, authentication).name
        }

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
