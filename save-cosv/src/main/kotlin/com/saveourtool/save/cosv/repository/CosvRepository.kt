package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.CosvMetadataDto
import com.saveourtool.save.entities.vulnerability.VulnerabilityExt
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.filters.VulnerabilityFilter

import com.saveourtool.osv4k.OsvSchema
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import kotlinx.serialization.KSerializer

typealias CosvSchema<D, A_E, A_D, A_R_D> = OsvSchema<D, A_E, A_D, A_R_D>
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
     * @param user [User] who saves COSV
     * @param organization [Organization] to which COSV is uploaded
     * @return [Mono] with metadata for save COSV
     */
    fun <D, A_E, A_D, A_R_D> save(
        entry: CosvSchema<D, A_E, A_D, A_R_D>,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
        user: User,
        organization: Organization?,
    ): Mono<CosvMetadataDto>

    /**
     * Finds entry with provided [CosvSchema.id] and max [CosvSchema.modified]
     *
     * @param cosvId
     * @param serializer [KSerializer] to decode entry from JSON
     * @return [Mono] with [CosvSchema]
     */
    fun <D, A_E, A_D, A_R_D> findLatestById(
        cosvId: String,
        serializer: CosvSchemaKSerializer<D, A_E, A_D, A_R_D>,
    ): CosvSchemaMono<D, A_E, A_D, A_R_D>

    /**
     * Finds extended raw cosv with [CosvSchema.id] and max [CosvSchema.modified]
     *
     * @param cosvId
     * @return [Mono] with [VulnerabilityExt]
     */
    fun findLatestRawExt(
        cosvId: String,
    ): Mono<VulnerabilityExt>

    /**
     * Finds metadata of cosv by [filter]
     *
     * @param filter
     * @return [Flux] with [VulnerabilityExt]
     */
    fun findRawExtByFilter(
        filter: VulnerabilityFilter,
    ): Flux<VulnerabilityExt>

    /**
     * @param cosvId
     * @param status
     * @return [VulnerabilityExt]
     */
    fun findLatestRawExtByCosvIdAndStatus(
        cosvId: String,
        status: VulnerabilityStatus,
    ): Mono<VulnerabilityExt>

    fun findAllLatestRawExtByUserName(
        userName: String,
    ): Flux<VulnerabilityExt>
}