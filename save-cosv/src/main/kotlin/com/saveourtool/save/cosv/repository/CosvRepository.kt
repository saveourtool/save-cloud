package com.saveourtool.save.cosv.repository

import com.saveourtool.osv4k.OsvSchema as CosvSchema
import reactor.core.publisher.Mono

import kotlinx.serialization.KSerializer

typealias CosvSchemaMono<D, A_E, A_D, A_R_D> = Mono<CosvSchema<D, A_E, A_D, A_R_D>>
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
     * @return empty [Mono]
     */
    fun <D, A_E, A_D, A_R_D> save(
        entry: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): Mono<Unit>

    /**
     * Finds entry with provided [CosvSchema.id] and max [CosvSchema.modified]
     *
     * @param id
     * @param serializer [KSerializer] to decode entry from JSON
     * @return [Mono] with [CosvSchema]
     */
    fun <D, A_E, A_D, A_R_D> findLatestById(
        id: String,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): CosvSchemaMono<D, A_E, A_D, A_R_D>
}
