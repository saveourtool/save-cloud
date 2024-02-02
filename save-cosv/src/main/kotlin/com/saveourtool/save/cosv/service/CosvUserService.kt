package com.saveourtool.save.cosv.service

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.cosv.storage.AvatarStorage
import com.saveourtool.save.domain.UserSaveStatus
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserStatus
import com.saveourtool.save.repository.LnkUserOrganizationRepository
import com.saveourtool.save.repository.LnkUserProjectRepository
import com.saveourtool.save.repository.OriginalLoginRepository
import com.saveourtool.save.repository.UserRepository
import com.saveourtool.save.service.UserService
import com.saveourtool.save.storage.AvatarKey
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.orNotFound
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Primary
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.scheduler.Schedulers

/**
 * Service for user
 */
@Service
@Suppress("LongParameterList")
@Primary
class CosvUserService(
    private val userRepository: UserRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val originalLoginRepository: OriginalLoginRepository,
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
    private val lnkUserProjectRepository: LnkUserProjectRepository,
    private val avatarStorage: AvatarStorage,
) : UserService(
    userRepository,
    applicationEventPublisher,
    lnkUserOrganizationRepository,
) {
    /**
     * @param name name of user
     * @param authentication
     * @return UserSaveStatus
     */
    @Transactional
    fun deleteUser(
        name: String,
        authentication: Authentication,
    ): UserSaveStatus {
        val user: User = userRepository.findByIdOrNull(authentication.userId()).orNotFound {
            "User with id ${authentication.userId()} not found in database"
        }
        val newName = "Deleted-${user.id}"
        userRepository.deleteHighLevelName(user.name)
        userRepository.saveHighLevelName(newName)
        userRepository.save(user.apply {
            this.name = newName
            this.status = UserStatus.DELETED
            this.avatar = null
            this.company = null
            this.twitter = null
            this.email = null
            this.gitHub = null
            this.linkedin = null
            this.location = null
        })

        val avatarKey = AvatarKey(
            AvatarType.USER,
            name,
        )
        avatarStorage.delete(avatarKey)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe()

        originalLoginRepository.deleteByUserId(user.requiredId())
        lnkUserProjectRepository.deleteByUserId(user.requiredId())
        lnkUserOrganizationRepository.deleteByUserId(user.requiredId())

        return UserSaveStatus.DELETED
    }
}
