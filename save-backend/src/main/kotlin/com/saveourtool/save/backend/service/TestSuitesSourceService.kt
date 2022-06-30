package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/**
 * Service for [com.saveourtool.save.entities.TestSuitesSource]
 */
@Service
class TestSuitesSourceService(
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
) {
    /**
     * @param name of [TestSuitesSource]
     * @return entity of [TestSuitesSource] or null
     */
    fun findByName(name: String) = testSuitesSourceRepository.findByName(name)

    /**
     * @param dto entity as DTO [TestSuitesSourceDto]
     * @return saved entity as [TestSuitesSource]
     */
    fun createNew(dto: TestSuitesSourceDto) = testSuitesSourceRepository.save(
        TestSuitesSource(
            type = dto.type,
            name = dto.name,
            description = dto.description,
            locationType = dto.locationType,
            locationInfo = dto.locationInfo,
        )
    )

    /**
     * @param name of [TestSuitesSource]
     * @return entity of [TestSuitesSource]
     * @throws ResponseStatusException entity not found
     */
    fun getByName(name: String) = findByName(name)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "TestSuitesSource (name=$name) not found")
}
