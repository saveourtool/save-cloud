package com.saveourtool.save.cosv.storage

import com.saveourtool.save.cosv.repository.RawCosvFileRepository
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.RawCosvFile
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.ReactiveStorageWithDatabase
import com.saveourtool.save.utils.blockingToFlux
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.orNotFound
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

typealias OrganizationAndOwner = Pair<Organization, User>

/**
 * S3 storage for [RawCosvFile]
 */
@Component
class RawCosvFileStorage(
    s3Operations: S3Operations,
    s3KeyManager: RawCosvFileS3KeyManager,
    repository: RawCosvFileRepository,
) : ReactiveStorageWithDatabase<RawCosvFileDto, RawCosvFile, RawCosvFileRepository, RawCosvFileS3KeyManager>(
    s3Operations = s3Operations,
    s3KeyManager = s3KeyManager,
    repository = repository,
) {
    /**
     * @param organizationName
     * @return all [RawCosvFileDto]s which has provided [RawCosvFile.organization]
     */
    fun listByOrganization(
        organizationName: String,
    ): Flux<RawCosvFileDto> = blockingToFlux {
        s3KeyManager.listByOrganization(organizationName)
    }

    /**
     * @param ids
     * @param newStatus
     * @return empty [Mono]
     */
    fun markAs(
        ids: Collection<Long>,
        newStatus: RawCosvFileStatus,
    ): Mono<Unit> = blockingToMono { s3KeyManager.markAs(ids, newStatus) }

    /**
     * @param id
     * @return [Organization] to which is uploaded and [User] who uploaded
     */
    fun getOrganizationAndOwner(
        id: Long,
    ): Mono<OrganizationAndOwner> = blockingToMono { s3KeyManager.getOrganizationAndOwner(id) }

    /**
     * @param id
     * @return content of raw COSV file
     */
    fun downloadById(
        id: Long,
    ): Flux<ByteBuffer> = blockingToMono {
        s3KeyManager.findKeyById(id)
    }
        .orNotFound { "Not found raw COSV file id=$id" }
        .flatMapMany { download(it) }

    /**
     * @param id
     * @return result of deletion
     */
    fun deleteById(
        id: Long,
    ): Mono<Boolean> = blockingToMono {
        s3KeyManager.findKeyById(id)
    }
        .orNotFound { "Not found raw COSV file id=$id" }
        .flatMap { delete(it) }
}