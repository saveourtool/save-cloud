package com.saveourtool.save.cosv.storage

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.RawCosvFile
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileDto.Companion.isDuplicate
import com.saveourtool.save.entities.cosv.RawCosvFileDto.Companion.isHasErrors
import com.saveourtool.save.entities.cosv.RawCosvFileDto.Companion.isProcessing
import com.saveourtool.save.entities.cosv.RawCosvFileDto.Companion.isUploadedJsonFile
import com.saveourtool.save.entities.cosv.RawCosvFileDto.Companion.isUploadedZipArchive
import com.saveourtool.save.entities.cosv.RawCosvFileStatisticsDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.DefaultStorageProjectReactor
import com.saveourtool.save.storage.ReactiveStorageWithDatabase
import com.saveourtool.save.storage.deleteUnexpectedKeys
import com.saveourtool.save.utils.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

typealias OrganizationAndOwner = Pair<Organization, User>
typealias RawCosvFileDtoCollection = Collection<RawCosvFileDto>

/**
 * S3 storage for [RawCosvFile]
 */
@Component
class RawCosvFileStorage(
    private val s3Operations: S3Operations,
    s3KeyManager: RawCosvFileS3KeyManager,
) : ReactiveStorageWithDatabase<RawCosvFileDto, RawCosvFile, RawCosvFileS3KeyManager>(
    s3Operations,
    s3KeyManager,
) {
    /**
     * Init method to remove deleted (unexpected) ids which are detected in storage, but missed in database
     */
    override fun doInit(underlying: DefaultStorageProjectReactor<RawCosvFileDto>): Mono<Unit> = Mono.fromFuture {
        s3Operations.deleteUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            s3KeyManager = s3KeyManager,
        )
    }
        .flatMap {
            underlying.list()
                .filter { it.status == RawCosvFileStatus.PROCESSED }
                .collectList()
                .flatMap { rawCosvFiles ->
                    underlying.deleteAll(rawCosvFiles)
                }
        }
        .flatMap {
            underlying.list()
                .filter { it.contentLength == null }
                .flatMap { key ->
                    underlying.contentLength(key).map { key to it }
                }
                .blockingMap { (key, contentLength) ->
                    s3KeyManager.updateKeyByContentLength(key, contentLength)
                }
                .thenJust(Unit)
        }
        .publishOn(s3Operations.scheduler)

    /**
     * @param organizationName
     * @param userName
     * @return statistics [RawCosvFileStatisticDto] for all [RawCosvFileDto]s which belongs to [organizationName] and uploaded by [userName]
     */
    fun statisticsByOrganizationAndUser(
        organizationName: String,
        userName: String,
    ): Mono<RawCosvFileStatisticsDto> = blockingToMono {
        val filesList = s3KeyManager.listByOrganizationAndUser(organizationName, userName).toList()
        RawCosvFileStatisticsDto(
            filesList.count(),
            filesList.count { it.isUploadedZipArchive() },
            filesList.count { it.isUploadedJsonFile() },
            filesList.count { it.isProcessing() },
            filesList.count { it.isDuplicate() },
            filesList.count { it.isHasErrors() }
        )
    }

    /**
     * @param organizationName
     * @param userName
     * @param pageRequest
     * @return all [RawCosvFileDto]s which belongs to [organizationName] and uploaded by [userName]
     */
    fun listByOrganizationAndUser(
        organizationName: String,
        userName: String,
        pageRequest: PageRequest? = null,
    ): Mono<RawCosvFileDtoCollection> = blockingToMono {
        s3KeyManager.listByOrganizationAndUser(organizationName, userName, pageRequest).toList()
    }

    /**
     * @param ids
     * @param newStatus
     * @return empty [Mono]
     */
    fun updateAll(
        ids: Collection<Long>,
        newStatus: RawCosvFileStatus,
    ): Mono<Unit> = blockingToMono { s3KeyManager.updateAll(ids, newStatus) }

    /**
     * @param id
     * @param newStatus
     * @param statusMessage
     * @return empty [Mono]
     */
    fun update(
        id: Long,
        newStatus: RawCosvFileStatus,
        statusMessage: String,
    ): Mono<Unit> = blockingToMono { s3KeyManager.update(id, newStatus, statusMessage) }

    /**
     * @param id
     * @return [Organization] to which is uploaded and [User] who uploaded
     */
    fun getOrganizationAndOwner(
        id: Long,
    ): Mono<OrganizationAndOwner> = blockingToMono { s3KeyManager.getOrganizationAndOwner(id) }

    /**
     * @param id
     * @return [RawCosvFileDto]
     */
    fun findById(
        id: Long,
    ): Mono<RawCosvFileDto> = blockingToMono { s3KeyManager.findKeyByEntityId(id) }
        .switchIfEmptyToNotFound { "Not found raw COSV file id=$id" }

    /**
     * @param id
     * @return content of raw COSV file
     */
    fun downloadById(
        id: Long,
    ): Flux<ByteBuffer> = findById(id)
        .flatMapMany { download(it) }

    /**
     * @param id
     * @return result of deletion
     */
    fun deleteById(
        id: Long,
    ): Mono<Boolean> = findById(id)
        .flatMap { delete(it) }
}
