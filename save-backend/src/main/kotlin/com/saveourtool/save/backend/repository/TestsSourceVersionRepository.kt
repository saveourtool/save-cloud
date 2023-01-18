package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.entities.TestsSourceVersion
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [TestsSourceVersion]
 */
@Repository
@Suppress(
    "IDENTIFIER_LENGTH",
    "FUNCTION_NAME_INCORRECT_CASE",
    "FunctionNaming",
    "FunctionName",
)
interface TestsSourceVersionRepository : BaseEntityRepository<TestsSourceVersion> {
    /**
     * @param snapshot
     * @param name
     * @return [TestsSourceVersion] found by [name] in provided [TestsSourceSnapshot]
     */
    fun findBySnapshotAndName(snapshot: TestsSourceSnapshot, name: String): TestsSourceVersion?

    /**
     * @param source
     * @param name
     * @return [TestsSourceVersion] found by [name] in provided [TestSuitesSource]
     */
    fun findBySnapshot_SourceAndName(source: TestSuitesSource, name: String): TestsSourceVersion?

    /**
     * @param source
     * @return all [TestsSourceVersion] in provided [TestSuitesSource]
     */
    fun findAllBySnapshot_Source(source: TestSuitesSource): Collection<TestsSourceVersion>

    /**
     * @param snapshot
     * @return all [TestsSourceVersion] which are linked to provide [TestsSourceSnapshot]
     */
    fun findAllBySnapshot(snapshot: TestsSourceSnapshot): Collection<TestsSourceVersion>

    /**
     * @param organizationName
     * @param sourceName
     * @param version
     * @return [TestsSourceVersion] which linked to some [TestsSourceSnapshot] (by [organizationName], [sourceName]) with provided [version]
     */
    fun findBySnapshot_Source_Organization_NameAndSnapshot_Source_NameAndName(
        organizationName: String,
        sourceName: String,
        version: String,
    ): TestsSourceVersion?

    /**
     * @param snapshotId
     * @param version
     * @return [TestsSourceVersion] which linked to some [TestsSourceSnapshot] (by [snapshotId]) with provided [version]
     */
    fun findBySnapshot_IdAndName(
        snapshotId: Long,
        version: String,
    ): TestsSourceVersion?

    /**
     * @param sourceId
     * @param version
     * @return [TestsSourceVersion] which linked to some [TestsSourceSnapshot] from some [com.saveourtool.save.entities.TestSuitesSource] (by [sourceId]) with provided [version]
     */
    fun findBySnapshot_SourceIdAndName(
        sourceId: Long,
        version: String,
    ): TestsSourceVersion?

    /**
     * @param organizationName
     * @return all [TestsSourceVersion] which linked to some [TestsSourceSnapshot] (by [organizationName])
     */
    fun findAllBySnapshot_Source_Organization_Name(
        organizationName: String,
    ): Collection<TestsSourceVersion>

    /**
     * @param organizationName
     * @param sourceName
     * @return all [TestsSourceVersion] which linked to some [TestsSourceSnapshot] (by [organizationName], [sourceName])
     */
    fun findAllBySnapshot_Source_Organization_NameAndSnapshot_Source_Name(
        organizationName: String,
        sourceName: String,
    ): Collection<TestsSourceVersion>
}
