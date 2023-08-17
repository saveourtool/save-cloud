package com.saveourtool.save.backend.service

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.backend.repository.LnkUserOrganizationRepository
import com.saveourtool.save.backend.repository.LnkUserProjectRepository
import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.storage.AvatarKey
import com.saveourtool.save.backend.storage.AvatarStorage
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.UserSaveStatus
import com.saveourtool.save.entities.OriginalLogin
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserStatus
import com.saveourtool.save.utils.AVATARS_PACKS_DIR
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.orNotFound

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalArgumentException

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
    private val avatarStorage: AvatarStorage,
) {
    /**
     * @param username
     * @return spring's UserDetails retrieved from save's user found by provided values
     */
    fun findByName(username: String) = blockingToMono {
        userRepository.findByName(username)
    }

    /**
     * @param username
     * @param source source (where the user identity is coming from)
     * @return spring's UserDetails retrieved from save's user found by provided values
     */
    fun findByOriginalLogin(username: String, source: String) = blockingToMono {
        originalLoginRepository.findByNameAndSource(username, source)?.user
    }

    /**
     * @param name
     * @throws NoSuchElementException
     */
    fun updateAvatarVersion(name: String): String {
        val user = userRepository.findByName(name).orNotFound()
        var version = if (user.avatar?.find { it == '?' } != null) {
            user.avatar!!.substringAfterLast("?").toInt()
        } else {
            0
        }
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
     * @param newUser
     * @param oldName
     * @param oldUserStatus
     * @return UserSaveStatus
     */
    @Transactional
    fun saveUser(newUser: User, oldName: String?, oldUserStatus: UserStatus): UserSaveStatus {
        val isNameFreeAndNotTaken = userRepository.validateName(newUser.name) != 0L
        // if we are registering new user (updating just name and status to ACTIVE):
        return if (oldUserStatus == UserStatus.CREATED && newUser.status == UserStatus.ACTIVE) {
            // checking if the user with new name already exists (it's definitely not our user, so if found -> CONFLICT)
            if (isNameFreeAndNotTaken) {
                userRepository.save(newUser)
                UserSaveStatus.UPDATE
            } else {
                UserSaveStatus.CONFLICT
            }
        } else {
            // we are trying to change the name of ACTIVE user
            oldName?.let {
                // but such name is already taken and exists in db
                if (isNameFreeAndNotTaken) {
                    userRepository.deleteHighLevelName(oldName)
                    userRepository.saveHighLevelName(newUser.name)
                    userRepository.save(newUser)
                    UserSaveStatus.UPDATE
                } else {
                    UserSaveStatus.CONFLICT
                }
                // if we are changing other fields of ACTIVE users, but not changing the name we can just save user
                // here we highly depend on the `oldName` field (from client code)
                // but we are safe as we have a unique constraint on the database
            } ?: run {
                userRepository.save(newUser)
                UserSaveStatus.UPDATE
            }
        }
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
        return userRepository.save(
            User(
                name = name,
                password = null,
                role = userRole,
                status = UserStatus.CREATED,
            )
        )
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

        val avatarKey = AvatarKey(
            AvatarType.USER,
            name,
        )
        avatarStorage.delete(avatarKey)

        originalLoginRepository.deleteByUserId(user.requiredId())
        lnkUserProjectRepository.deleteByUserId(user.requiredId())
        lnkUserOrganizationRepository.deleteByUserId(user.requiredId())

        return UserSaveStatus.DELETED
    }

    companion object {
        private const val UNIQUE_NAME_SEPARATOR = "_"
    }
}
