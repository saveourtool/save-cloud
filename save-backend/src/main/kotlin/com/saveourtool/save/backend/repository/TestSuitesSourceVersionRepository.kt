package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestSuitesSourceSnapshot
import com.saveourtool.save.entities.TestSuitesSourceVersion
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [TestSuitesSourceVersion]
 */
@Repository
@Suppress(
    "IDENTIFIER_LENGTH",
    "FUNCTION_NAME_INCORRECT_CASE",
    "FunctionNaming",
    "FunctionName",
)
interface TestSuitesSourceVersionRepository : BaseEntityRepository<TestSuitesSourceVersion> {
    /**
     * @param snapshot
     * @param name
     * @return [TestSuitesSourceVersion] found by [name] in provided [TestSuitesSourceSnapshot]
     */
    fun findBySnapshotAndName(snapshot: TestSuitesSourceSnapshot, name: String): TestSuitesSourceVersion?

    /**
     * @param source
     * @param name
     * @return [TestSuitesSourceVersion] found by [name] in provided [TestSuitesSource]
     */
    fun findBySnapshot_SourceAndName(source: TestSuitesSource, name: String): TestSuitesSourceVersion?

    /**
     * @param source
     * @return all [TestSuitesSourceVersion] in provided [TestSuitesSource]
     */
    fun findAllBySnapshot_Source(source: TestSuitesSource): Collection<TestSuitesSourceVersion>

    /**
     * @param snapshot
     * @return all [TestSuitesSourceVersion] which are linked to provide [TestSuitesSourceSnapshot]
     */
    fun findAllBySnapshot(snapshot: TestSuitesSourceSnapshot): Collection<TestSuitesSourceVersion>
}
