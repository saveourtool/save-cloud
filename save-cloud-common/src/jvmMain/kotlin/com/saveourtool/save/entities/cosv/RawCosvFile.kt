package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.User
import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.spring.entity.IBaseEntityWithDate

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Entity for table `raw_cosv_file`
 *
 * @property fileName
 * @property user
 * @property organization
 * @property status
 * @property createDate
 * @property updateDate
 * @property statusMessage
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
    @Enumerated
    var status: RawCosvFileStatus,
    var statusMessage: String? = null,
    override var createDate: LocalDateTime? = null,
    override var updateDate: LocalDateTime? = null,
) : BaseEntityWithDtoWithId<RawCosvFileDto>(), IBaseEntityWithDate {
    override fun toDto(): RawCosvFileDto = RawCosvFileDto(
        fileName = fileName,
        userName = user.name,
        organizationName = organization.name,
        status = status,
        statusMessage = statusMessage,
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
        )
    }
}
