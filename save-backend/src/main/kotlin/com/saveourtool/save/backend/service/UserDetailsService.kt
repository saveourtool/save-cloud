package com.saveourtool.save.backend.service

import com.saveourtool.save.authservice.utils.toSpringUserDetails
import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.backend.repository.LnkUserOrganizationRepository
import com.saveourtool.save.backend.repository.LnkUserProjectRepository
import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.UserSaveStatus
import com.saveourtool.save.entities.OriginalLogin
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserStatus
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.orNotFound

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.util.*

/**
 * A service that provides access to [UserRepository] and [OriginalLoginRepository]
 */
@Service
class UserDetailsService(
    private val userRepository: UserRepository,
    private val originalLoginRepository: OriginalLoginRepository,
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
    private val lnkUserProjectRepository: LnkUserProjectRepository,
) {
    /**
     * @param username
     * @return spring's UserDetails retrieved from save's user found by provided values
     */
    fun findByName(username: String) = blockingToMono {
        userRepository.findByName(username)
    }
        .map { it.toSpringUserDetails() }

    /**
     * @param username
     * @param source source (where the user identity is coming from)
     * @return spring's UserDetails retrieved from save's user found by provided values
     */
    fun findByOriginalLogin(username: String, source: String) = blockingToMono {
        originalLoginRepository.findByNameAndSource(username, source)?.user
    }
        .map { it.toSpringUserDetails() }

    /**
     * @param name
     * @throws NoSuchElementException
     */
    fun updateAvatarVersion(name: String) {
        val user = userRepository.findByName(name).orNotFound()
        var version = user.avatar?.substringAfterLast("?")?.toInt() ?: 0

        user.apply {
            avatar = "${AvatarType.USER.toUrlStr(name)}?${++version}"
        }
        user.let { userRepository.save(it) }
    }

    /**
     * @param authentication
     * @return global [Role] of authenticated user
     */
    fun getGlobalRole(authentication: Authentication): Role = authentication.authorities
        .map { grantedAuthority ->
            Role.values().find { role -> role.asSpringSecurityRole() == grantedAuthority.authority }
        }
        .sortedBy { it?.priority }
        .lastOrNull()
        ?: Role.VIEWER

    /**
     * @param user
     * @param oldName
     * @return UserSaveStatus
     */
    @Transactional
    fun saveUser(user: User, oldName: String?): UserSaveStatus = if (oldName == null) {
        userRepository.save(user)
        UserSaveStatus.UPDATE
    } else if (userRepository.validateName(user.name) != 0L) {
        userRepository.deleteHighLevelName(oldName)
        userRepository.saveHighLevelName(user.name)
        userRepository.save(user)
        UserSaveStatus.UPDATE
    } else {
        UserSaveStatus.CONFLICT
    }

    /**
     * @param userNameCandidate
     * @param userRole
     * @return created [User]
     */
    @Transactional
    fun saveNewUser(userNameCandidate: String, userRole: String): User {
        val existedUser = userRepository.findByName(userNameCandidate)
        val name = existedUser?.let {
            val prefix = "$userNameCandidate$UNIQUE_NAME_SEPARATOR"
            val suffix = userRepository.findByNameStartingWith(prefix)
                .map { it.name.replace(prefix, "") }
                .mapNotNull { it.toIntOrNull() }
                .maxOrNull()
                ?.inc()
                ?: 1
            "$prefix$suffix"
        } ?: run {
            userNameCandidate
        }
        return userRepository.save(User(
            name = name,
            password = null,
            role = userRole,
            status = UserStatus.CREATED,
        ))
    }

    /**
     * @param user
     * @param nameInSource
     * @param source
     */
    @Transactional
    fun addSource(user: User, nameInSource: String, source: String) {
        originalLoginRepository.save(OriginalLogin(nameInSource, user, source))
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
        val user: User = userRepository.findByName(name).orNotFound()
        val newName = "Deleted-${user.id}"
        if (user.id == authentication.userId()) {
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
        } else {
            return UserSaveStatus.CONFLICT
        }

        originalLoginRepository.deleteByUserId(user.requiredId())
        lnkUserProjectRepository.deleteByUserId(user.requiredId())
        lnkUserOrganizationRepository.deleteByUserId(user.requiredId())

        return UserSaveStatus.DELETED
    }

    companion object {
        private const val UNIQUE_NAME_SEPARATOR = "_"
    }
}
