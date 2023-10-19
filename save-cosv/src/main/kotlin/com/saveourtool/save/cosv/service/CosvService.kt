package com.saveourtool.save.cosv.service

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.cosv.processor.CosvProcessor
import com.saveourtool.save.cosv.repository.CosvGeneratedIdRepository
import com.saveourtool.save.cosv.repository.CosvRepository
import com.saveourtool.save.cosv.repository.CosvSchema
import com.saveourtool.save.cosv.repository.LnkVulnerabilityMetadataTagRepository
import com.saveourtool.save.cosv.storage.RawCosvFileStorage
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.*
import com.saveourtool.save.entities.vulnerability.VulnerabilityDto
import com.saveourtool.save.utils.*

import com.saveourtool.osv4k.*
import com.saveourtool.osv4k.RawOsvSchema as RawCosvSchema
import org.slf4j.Logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.extra.math.sumAll

import java.nio.ByteBuffer

import kotlinx.serialization.serializer
import reactor.core.scheduler.Schedulers
import javax.annotation.PostConstruct
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Service for vulnerabilities
 */
@Service
@Suppress("LongParameterList")
class CosvService(
    private val rawCosvFileStorage: RawCosvFileStorage,
    private val cosvRepository: CosvRepository,
    private val backendService: IBackendService,
    private val cosvProcessor: CosvProcessor,
    private val vulnerabilityMetadataService: VulnerabilityMetadataService,
    private val vulnerabilityRatingService: VulnerabilityRatingService,
    private val lnkVulnerabilityMetadataTagRepository: LnkVulnerabilityMetadataTagRepository,
    private val cosvGeneratedIdRepository: CosvGeneratedIdRepository,
) {
    @PostConstruct
    fun restoreProcessing() {
        waitReactivelyUntil(
            interval = rawStorageCheckingInterval,
            numberOfChecks = (rawStorageInitTime / rawStorageCheckingInterval).toLong(),
        ) {
            rawCosvFileStorage.isInitDone()
        }
            .flatMap { isInitDone ->
                if (isInitDone) {
                    doRestoreProcessing()
                        .map {
                            log.info {
                                "Processed all ${RawCosvFileStatus.IN_PROGRESS} files from storage ${RawCosvFileStorage::class.simpleName} after restart"
                            }
                        }
                } else {
                    log.warn {
                        "Storage ${RawCosvFileStorage::class.simpleName} is not initialized in ${rawStorageInitTime}"
                    }
                    Mono.empty()
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe()
    }

    private fun doRestoreProcessing(): Mono<Unit> = rawCosvFileStorage.list()
        .filter { it.status == RawCosvFileStatus.IN_PROGRESS }
        .groupBy { rawCosvFile ->
            rawCosvFile.userName to rawCosvFile.organizationName
        }
        .flatMap { groupedFlux ->
            val (userName, organizationName) = groupedFlux.key()
            blockingToMono {
                backendService.getUserByName(userName) to backendService.getOrganizationByName(organizationName)
            }
                .flatMap { (user, organization) ->
                    groupedFlux
                        .flatMap {
                            doProcess(it.requiredId(), user, organization)
                        }
                        .updateRating(user, organization)
                }
        }
        .thenJust(Unit)

    /**
     * @return generated identifier for COSV
     */
    @Transactional
    fun generateIdentifier(): String = cosvGeneratedIdRepository.saveAndFlush(CosvGeneratedId()).getIdentifier()

    /**
     * @param rawCosvFileIds
     * @param user
     * @param organization
     * @return empty [Mono]
     */
    fun process(
        rawCosvFileIds: Collection<Long>,
        user: User,
        organization: Organization,
    ): Mono<Unit> = rawCosvFileIds.toFlux()
        .flatMap { rawCosvFileId ->
            validateUserAndOrganization(rawCosvFileId, user, organization)
                .flatMap {
                    doProcess(rawCosvFileId, user, organization)
                }
        }
        .updateRating(user, organization)
        .map {
            log.debug {
                "Finished processing raw COSV files $rawCosvFileIds"
            }
        }

    private fun validateUserAndOrganization(
        rawCosvFileId: Long,
        user: User,
        organization: Organization,
    ): Mono<*> = rawCosvFileStorage.getOrganizationAndOwner(rawCosvFileId)
        .filter { (organizationForRawCosvFile, userForRawCosvFile) ->
            organization.requiredId() == organizationForRawCosvFile.requiredId() && user.requiredId() == userForRawCosvFile.requiredId()
        }
        .switchIfEmpty {
            log.error {
                "Submitter ${user.name} is not the owner of the raw cosv file id=$rawCosvFileId or submitted to another organization ${organization.name}"
            }
            Mono.empty()
        }

    private fun doProcess(
        rawCosvFileId: Long,
        user: User,
        organization: Organization,
    ): Mono<Int> = rawCosvFileStorage.downloadById(rawCosvFileId)
        .collectToInputStream()
        .flatMap { inputStream ->
            val errorMessage by lazy {
                "Failed to process raw COSV file with id: $rawCosvFileId"
            }
            Mono.fromCallable {
                cosvProcessor.decode(inputStream)
            }
                .flatMapIterable { it }
                .flatMap { save(it, user, organization, isAutoApprove = true) }
                .onErrorResume { error ->
                    val cause = error.firstCauseOrThis()
                    rawCosvFileStorage.update(rawCosvFileId, RawCosvFileStatus.FAILED, "$errorMessage is due to ${cause.message}")
                        .then(Mono.error(error))
                }
                .collectList()
                .flatMap { metadataList ->
                    rawCosvFileStorage.update(
                        rawCosvFileId,
                        RawCosvFileStatus.PROCESSED,
                        "Processed as ${metadataList.map(VulnerabilityMetadataDto::identifier)}"
                    )
                        .flatMap {
                            rawCosvFileStorage.deleteById(rawCosvFileId)
                        }
                        .thenReturn(metadataList.size)
                }
                .onErrorResume { error ->
                    log.error(error) { errorMessage }
                    Mono.just(0)
                }
        }

    private fun Flux<Int>.updateRating(
        user: User,
        organization: Organization,
    ): Mono<Unit> = sumAll()
        .blockingMap {
            vulnerabilityRatingService.addRatingForBulkUpload(user, organization, it)
        }

    /**
     * @param cosvId
     * @param updater
     * @return [Mono] with new metadata
     */
    fun update(
        cosvId: String,
        updater: (RawCosvSchema) -> Mono<RawCosvSchema>,
    ): Mono<VulnerabilityMetadataDto> = getVulnerabilityExt(cosvId)
        .blockingMap { rawCosvExt ->
            rawCosvExt to Pair(
                backendService.getUserByName(rawCosvExt.metadataDto.user.name),
                rawCosvExt.metadataDto.organization?.let { organization ->
                    backendService.getOrganizationByName(organization.name)
                }
            )
        }
        .flatMap { (rawCosvExt, infoFromDatabase) ->
            val (owner, organization) = infoFromDatabase
            updater(rawCosvExt.cosv)
                .flatMap { newCosv ->
                    save(
                        cosv = newCosv.copy(modified = getCurrentLocalDateTime().truncatedToMills()),
                        user = owner,
                        organization = organization,
                    )
                }
        }

    /**
     * @param cosv
     * @param user
     * @param organization
     * @return metadata for saved COSV
     */
    fun saveManual(
        cosv: ManualCosvSchema,
        user: User,
        organization: Organization?,
    ): Mono<VulnerabilityMetadataDto> = save(cosv, user, organization)

    private inline fun <reified D, reified A_E, reified A_D, reified A_R_D> save(
        cosv: CosvSchema<D, A_E, A_D, A_R_D>,
        user: User,
        organization: Organization?,
        isAutoApprove: Boolean = false,
    ): Mono<VulnerabilityMetadataDto> = cosvRepository.save(cosv, serializer())
        .flatMap { key ->
            blockingToMono {
                vulnerabilityMetadataService.createOrUpdate(key, cosv, user, organization, isAutoApprove).toDto()
            }
                .onErrorResume { error ->
                    log.error(error) {
                        "Failed to save/update metadata for $key, cleaning up"
                    }
                    cosvRepository.delete(key).then(Mono.error(error))
                }
        }

    /**
     * Finds extended raw cosv with [CosvSchema.id] and max [CosvSchema.modified]
     *
     * @param identifier
     * @return [Mono] with [VulnerabilityExt]
     */
    fun getVulnerabilityExt(identifier: String): Mono<VulnerabilityExt> = blockingToMono { vulnerabilityMetadataService.findByIdentifier(identifier) }
        .flatMap { metadata ->
            cosvRepository.download(metadata.latestCosvFile, serializer<RawCosvSchema>()).blockingMap { content ->
                val tags = lnkVulnerabilityMetadataTagRepository
                    .findAllByVulnerabilityMetadataIdentifier(identifier)
                    .map { it.tag.name }
                    .toSet()

                VulnerabilityExt(
                    metadataDto = metadata.toDto().copy(tags = tags),
                    cosv = content,
                    // FixMe: need to fix bug here when mapping is empty
                    saveContributors = content.getSaveContributes().map { backendService.getUserByName(it.name).toUserInfo() },
                )
            }
        }

    /**
     * @param identifier
     * @return [Flux] of [ByteBuffer] with COSV's content
     */
    fun getVulnerabilityAsCosvStream(identifier: String): Flux<ByteBuffer> = blockingToMono { vulnerabilityMetadataService.findByIdentifier(identifier) }
        .flatMapMany { metadata -> cosvRepository.downloadAsStream(metadata.latestCosvFile) }

    /**
     * @param cosvFileId
     * @return [Flux] of [ByteBuffer] with COSV's content
     */
    fun getVulnerabilityVersionAsCosvStream(cosvFileId: Long): Flux<ByteBuffer> = cosvRepository.downloadAsStream(cosvFileId)

    /**
     * @param identifier
     * @return list of cosv files
     */
    fun listVersions(identifier: String): Flux<CosvFileDto> = cosvRepository.listVersions(identifier)

    companion object {
        private val log: Logger = getLogger<CosvService>()
        private val rawStorageCheckingInterval = 1.seconds
        private val rawStorageInitTime = 5.minutes

        private fun Throwable.firstCauseOrThis(): Throwable = generateSequence(this, Throwable::cause).last()
    }
}
