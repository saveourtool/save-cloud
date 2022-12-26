package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.File
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

@Repository
interface FileRepository : BaseEntityRepository<File> {
}