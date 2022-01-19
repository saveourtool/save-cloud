package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Test
import org.springframework.stereotype.Repository

import java.util.Optional

/**
 * Repository of tests
 */
@Repository
@Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
interface TestRepository : BaseEntityRepository<Test> {
    fun findByHashAndFilePathAndTestSuiteIdAndPluginName(hash: String, filePath: String, testSuiteId: Long, pluginName: String): Optional<Test>

    fun findAllByTestSuiteId(testSuiteId: Long): List<Test>
}
