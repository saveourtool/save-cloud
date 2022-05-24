package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.benchmarks.AwesomeBenchmarks
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * Repository of tests
 */
@Repository
@Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
interface AwesomeBenchmarksRepository : BaseEntityRepository<AwesomeBenchmarks>, JpaSpecificationExecutor<AwesomeBenchmarks>
