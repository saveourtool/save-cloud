package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.cosv.RawCosvFile
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

/**
 * A repository for [RawCosvFile]
 */
@Repository
interface RawCosvFileRepository : BaseEntityRepository<RawCosvFile> {
    /**
     * @param organizationName name from [RawCosvFile.organization]
     * @param userName name from [RawCosvFile.user]
     * @param fileName [RawCosvFile.fileName]
     * @return found [RawCosvFile] by provided values
     */
    fun findByOrganizationNameAndUserNameAndFileName(organizationName: String, userName: String, fileName: String): RawCosvFile?

    /**
     * @param organizationName name from [RawCosvFile.organization]
     * @param userName name from [RawCosvFile.user]
     * @return all [RawCosvFile]s which has provided [RawCosvFile.organization]
     */
    fun findAllByOrganizationNameAndUserName(organizationName: String, userName: String): Collection<RawCosvFile>

    /**
     * @param organizationName name from [RawCosvFile.organization]
     * @param userName name from [RawCosvFile.user]
     * @param pageRequest
     * @return all [RawCosvFile]s which has provided [RawCosvFile.organization]
     */
    fun findAllByOrganizationNameAndUserName(organizationName: String, userName: String, pageRequest: PageRequest): Collection<RawCosvFile>
}
