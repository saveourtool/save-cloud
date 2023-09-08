package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.cosv.RawCosvFile
import com.saveourtool.save.spring.repository.BaseEntityRepository

/**
 * A repository for [RawCosvFile]
 */
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
     * @return all [RawCosvFile]s which has provided [RawCosvFile.organization]
     */
    fun findAllByOrganizationName(organizationName: String): Collection<RawCosvFile>
}