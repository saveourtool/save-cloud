package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.VulnerabilityExt
import com.saveourtool.save.entities.cosv.VulnerabilityMetadataDto

import com.saveourtool.osv4k.OsvSchema
import com.saveourtool.save.entities.cosv.CosvFile
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.KSerializer

typealias CosvSchema<D, A_E, A_D, A_R_D> = OsvSchema<D, A_E, A_D, A_R_D>
typealias AnyCosvSchema = CosvSchema<*, *, *, *>
@Suppress("TYPEALIAS_NAME_INCORRECT_CASE")
typealias CosvSchemaKSerializer<D, A_E, A_D, A_R_D> = KSerializer<CosvSchema<D, A_E, A_D, A_R_D>>

/**
 * A repository for COSV
 */
interface CosvRepository {
    /**
     * Saves [entry] in repository
     *
     * @param entry
     * @param serializer [KSerializer] to encode [entry] to JSON
     * @return [Mono] with metadata for save COSV
     */
    fun <D, A_E, A_D, A_R_D> save(
        entry: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): Mono<CosvFile>

    fun <D, A_E, A_D, A_R_D> download(
        cosvFile: CosvFile,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): Mono<CosvSchema<D, A_E, A_D, A_R_D>>
    /**
     * Finds extended raw cosv with [CosvSchema.id] and max [CosvSchema.modified]
     *
     * @param identifier
     * @return [Mono] with [VulnerabilityExt]
     */
    fun findLatestExt(
        identifier: String,
    ): Mono<VulnerabilityExt>

    /**
     * @param identifier
     * @return [Flux] with removed versions
     */
    fun deleteAll(
        identifier: String,
    ): Flux<LocalDateTime>
}
