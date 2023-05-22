package com.saveourtool.save.backend.repository.contest

import com.saveourtool.save.entities.ContestSample
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * The repository of contest sample entities
 */
@Repository
interface ContestSampleRepository : BaseEntityRepository<ContestSample>
