package com.saveourtool.save.cosv.storage

import com.saveourtool.save.cosv.repository.RawCosvFileRepository
import com.saveourtool.save.cosv.service.OrganizationService
import com.saveourtool.save.cosv.service.UserService
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.entitiescosv.RawCosvFile
import com.saveourtool.save.entitiescosv.RawCosvFile.Companion.toNewEntity
import com.saveourtool.save.s3.S3OperationsProperties
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.storage.key.AbstractS3KeyDtoManager
import com.saveourtool.save.utils.BlockingBridge
import com.saveourtool.save.utils.getByIdOrNotFound

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * S3KeyManager for [RawCosvFileStorage]
 */
@Component
class RawCosvFileS3KeyManager(
    s3OperationsPropertiesProvider: S3OperationsProperties.Provider,
    rawCosvFileRepository: RawCosvFileRepository,
    private val userService: UserService,
    private val organizationService: OrganizationService,
    blockingBridge: BlockingBridge,
) : AbstractS3KeyDtoManager<RawCosvFileDto, RawCosvFile, RawCosvFileRepository>(
    concatS3Key(s3OperationsPropertiesProvider.s3Storage.prefix, "raw-cosv"),
    rawCosvFileRepository,
    blockingBridge,
) {
    override fun findByDto(dto: RawCosvFileDto): RawCosvFile? = repository.findByOrganizationNameAndUserNameAndFileName(
        organizationName = dto.organizationName,
        userName = dto.userName,
        fileName = dto.fileName,
    )

    override fun createNewEntityFromDto(dto: RawCosvFileDto): RawCosvFile =
            dto.toNewEntity(userService::getUserByName, organizationService::getOrganizationByName)

    /**
     * @param organizationName
     * @param userName
     * @param pageRequest
     * @return all [RawCosvFileDto]s which has provided [RawCosvFileDto.organizationName] and [RawCosvFileDto.userName]
     */
    fun listByOrganizationAndUser(
        organizationName: String,
        userName: String,
        pageRequest: PageRequest? = null,
    ): Collection<RawCosvFileDto> = run {
        pageRequest?.let { repository.findAllByOrganizationNameAndUserName(organizationName, userName, it) }
            ?: repository.findAllByOrganizationNameAndUserName(organizationName, userName)
    }.map { it.toDto() }

    /**
     * @param ids
     * @param newStatus
     */
    @Transactional
    fun updateAll(
        ids: Collection<Long>,
        newStatus: RawCosvFileStatus,
    ) {
        repository.saveAll(repository.findAllById(ids).map { entry -> entry.apply { status = newStatus } })
    }

    /**
     * @param id
     * @param newStatus
     * @param statusMessage
     */
    @Transactional
    fun update(
        id: Long,
        newStatus: RawCosvFileStatus,
        statusMessage: String?,
    ) {
        repository.save(
            repository.getByIdOrNotFound(id).apply {
                status = newStatus
                statusMessage?.let { this.statusMessage = it }
            }
        )
    }

    /**
     * @param id
     * @return [Organization] to which is uploaded and [User] who uploaded
     */
    fun getOrganizationAndOwner(
        id: Long,
    ): Pair<Organization, User> = repository.getByIdOrNotFound(id).let {
        it.organization to it.user
    }

    @Transactional
    override fun updateKeyByContentLength(
        key: RawCosvFileDto,
        contentLength: Long,
    ): RawCosvFileDto = key.contentLength
        ?.let { key }
        ?: run {
            repository.getByIdOrNotFound(key.requiredId())
                .let { entity ->
                    repository.save(entity.apply { this.contentLength = contentLength }).toDto()
                }
        }
}
