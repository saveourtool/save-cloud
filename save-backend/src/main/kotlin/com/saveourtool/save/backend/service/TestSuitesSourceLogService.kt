package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestSuitesSourceLogRepository
import com.saveourtool.save.entities.TestSuitesSourceLog
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import org.springframework.stereotype.Service

/**
 * Service for [TestSuitesSourceLog]
 */
@Service
class TestSuitesSourceLogService(
    private val testSuitesSourceLogRepository: TestSuitesSourceLogRepository,
    private val testSuitesSourceService: TestSuitesSourceService,
) {
    /**
     * @param sourceDto
     * @param version
     * @return [TestSuitesSourceLog] found by provided values or null
     */
    fun findByVersion(sourceDto: TestSuitesSourceDto, version: String): TestSuitesSourceLog? =
            testSuitesSourceLogRepository.findBySourceIdAndVersion(testSuitesSourceService.getByName(sourceDto.name).requiredId(), version)
                .orElse(null)

    /**
     * @param sourceDto
     * @param version
     * @return true if there is [TestSuitesSourceLog] with provided values
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun containsVersion(sourceDto: TestSuitesSourceDto, version: String): Boolean =
            findByVersion(sourceDto, version) != null

    /**
     * @param sourceDto
     * @param version
     * @return new instance of [TestSuitesSourceLog] created for provided values
     */
    fun createNew(sourceDto: TestSuitesSourceDto, version: String): TestSuitesSourceLog =
            testSuitesSourceLogRepository.save(TestSuitesSourceLog(source = testSuitesSourceService.getByName(sourceDto.name), version = version))
}
