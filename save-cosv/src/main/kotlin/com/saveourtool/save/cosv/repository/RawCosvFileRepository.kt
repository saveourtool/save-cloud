package com.saveourtool.save.cosv.repository

import com.saveourtool.save.entities.cosv.RawCosvFile
import com.saveourtool.save.spring.repository.BaseEntityRepository

/**
 * A repository for [RawCosvFile]
 */
interface RawCosvFileRepository : BaseEntityRepository<RawCosvFile> {
    /**
     * @param userName name from [RawCosvFile.user]
     * @param fileName [RawCosvFile.fileName]
     * @return found [RawCosvFile] by provided values
     */
    fun findByUserNameAndFileName(userName: String, fileName: String): RawCosvFile?
}
