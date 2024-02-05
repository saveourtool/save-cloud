package com.saveourtool.save.backend.service

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.backend.storage.AvatarStorage
import com.saveourtool.save.domain.UserSaveStatus
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserStatus
import com.saveourtool.save.repository.LnkUserOrganizationRepository
import com.saveourtool.save.repository.LnkUserProjectRepository
import com.saveourtool.save.repository.OriginalLoginRepository
import com.saveourtool.save.repository.UserRepository
import com.saveourtool.save.service.UserService
import com.saveourtool.save.storage.AvatarKey
import com.saveourtool.save.utils.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Primary
import org.springframework.data.repository.findByIdOrNull

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.scheduler.Schedulers
import java.lang.IllegalArgumentException

import java.util.*

/**
 * A service that provides access to [UserRepository] and [OriginalLoginRepository]
 */
@Service
@Primary
class UserDetailsService(
    private val userRepository: UserRepository,
    private val originalLoginRepository: OriginalLoginRepository,
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
    private val lnkUserProjectRepository: LnkUserProjectRepository,
    private val avatarStorage: AvatarStorage,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : UserService(
    userRepository,
    applicationEventPublisher,
    lnkUserOrganizationRepository,
    originalLoginRepository,
) {
    /**
     * We change the version just to work-around the caching on the frontend
     *
     * @param name
     * @return the id (version) of new avatar
     * @throws NoSuchElementException
     */
    fun updateAvatarVersion(name: String): String {
        val user = userRepository.findByName(name).orNotFound()
        var version = user.avatar?.find { it == '?' }?.let {
            user.avatar?.substringAfterLast("?")?.toInt() ?: 0
        } ?: 0
        val newAvatar = "${AvatarType.USER.toUrlStr(name)}?${++version}"
        user.apply { avatar = newAvatar }
            .let { userRepository.save(it) }

        return newAvatar
    }

    /**
     * Only for static resources!
     *
     * @param name
     * @param resource
     * @throws IllegalArgumentException
     */
    fun setAvatarFromResource(name: String, resource: String) {
        if (!resource.startsWith(AVATARS_PACKS_DIR)) {
            throw IllegalArgumentException("Only avatars from $AVATARS_PACKS_DIR can be set for user $name")
        }

        userRepository.findByName(name)
            .orNotFound()
            .apply { avatar = resource }
            .let { userRepository.save(it) }
    }

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
