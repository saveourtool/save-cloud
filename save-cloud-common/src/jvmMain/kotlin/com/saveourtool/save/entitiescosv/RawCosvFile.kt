package com.saveourtool.save.entitiescosv

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.spring.entity.IBaseEntityWithDate
import com.saveourtool.save.utils.ZIP_ARCHIVE_EXTENSION

import org.hibernate.annotations.Formula

import java.time.LocalDateTime
import javax.persistence.*

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Entity for table `raw_cosv_file`
 *
 * @property fileName
 * @property user
 * @property organization
 * @property status
 * @property statusMessage
 * @property contentLength
 * @property createDate
 * @property updateDate
 * @property isZip
 */
@Entity
@Suppress("LongParameterList")
class RawCosvFile(
    var fileName: String,
    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,
    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,
    @Enumerated(EnumType.STRING)
    var status: RawCosvFileStatus,
    @Formula("LOWER(file_name) LIKE '%_$ZIP_ARCHIVE_EXTENSION'")
    var isZip: Boolean? = null,
    var statusMessage: String? = null,
    var contentLength: Long? = null,
    override var createDate: LocalDateTime? = null,
    override var updateDate: LocalDateTime? = null,
) : BaseEntityWithDtoWithId<RawCosvFileDto>(), IBaseEntityWithDate {
    override fun toDto(): RawCosvFileDto = RawCosvFileDto(
        fileName = fileName,
        userName = user.name,
        organizationName = organization.name,
        status = status,
        statusMessage = statusMessage,
        contentLength = contentLength,
        updateDate = requiredUpdateDate().toKotlinLocalDateTime(),
        id = requiredId(),
    )

    companion object {
        /**
         * @param userResolver
         * @param organizationResolver
         * @return [RawCosvFile] from [RawCosvFileDto]
         */
        fun RawCosvFileDto.toNewEntity(
            userResolver: (String) -> User,
            organizationResolver: (String) -> Organization,
        ): RawCosvFile = RawCosvFile(
            fileName = fileName,
            user = userResolver(userName),
            organization = organizationResolver(organizationName),
            status = status,
            contentLength = contentLength,
        )
    }
}
