package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestSuitesSourceVersion
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [TestSuitesSourceVersion]
 */
@Repository
interface TestSuitesSourceVersionRepository : BaseEntityRepository<TestSuitesSourceVersion> {
    /**
     * @param testSuitesSource
     * @param name
     * @return [TestSuitesSourceVersion] found by [name] in [TestSuitesSource]
     */
    fun findBySnapshot_SourceAndName(testSuitesSource: TestSuitesSource, name: String): TestSuitesSourceVersion?
}
