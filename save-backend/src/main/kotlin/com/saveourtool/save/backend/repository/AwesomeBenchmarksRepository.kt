package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.benchmarks.AwesomeBenchmarks
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of tests
 */
@Repository
@Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
interface AwesomeBenchmarksRepository : BaseEntityRepository<AwesomeBenchmarks>
