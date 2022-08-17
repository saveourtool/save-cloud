package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.OriginalLogin
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OriginalLoginRepository: BaseEntityRepository<OriginalLogin> {
    /**
     * @param username
     * @return OriginalLogin or null if no results have been found
     */
    fun findByName(username: String): Optional<OriginalLogin>
}