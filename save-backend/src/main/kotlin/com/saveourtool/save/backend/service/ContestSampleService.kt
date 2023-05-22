package com.saveourtool.save.backend.service

import com.saveourtool.save.authservice.utils.AuthenticationDetails
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.repository.contest.ContestSampleFieldRepository
import com.saveourtool.save.backend.repository.contest.ContestSampleRepository
import com.saveourtool.save.entities.ContestSample
import com.saveourtool.save.entities.ContestSampleField
import com.saveourtool.save.entities.contest.ContestSampleDto
import com.saveourtool.save.utils.getByIdOrNotFound
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for contests sample
 *
 * @property contestSampleRepository
 * @property contestSampleFieldRepository
 * @property userRepository
 */
@Service
class ContestSampleService(
    private val contestSampleRepository: ContestSampleRepository,
    private val contestSampleFieldRepository: ContestSampleFieldRepository,
    private val userRepository: UserRepository,
) {
    /**
     * @param contestSampleDto dto of new vulnerability
     * @param authentication auth info of a current user
     */
    @Transactional
    fun save(
        contestSampleDto: ContestSampleDto,
        authentication: Authentication,
    ) {
        val userId = (authentication.details as AuthenticationDetails).id
        val user = userRepository.getByIdOrNotFound(userId)
        val contestSample = ContestSample(
            name = contestSampleDto.name,
            description = contestSampleDto.description,
            user = user,
        )

        val contestSampleNew = contestSampleRepository.saveAndFlush(contestSample)

        val contestSampleFields = contestSampleDto.fields.map {
            ContestSampleField(
                contestSample = contestSampleNew,
                name = it.name,
                type = it.type,
                userId = user.requiredId(),
            )
        }

        contestSampleFieldRepository.saveAll(contestSampleFields)
    }
}
