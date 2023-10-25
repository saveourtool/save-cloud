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
import com.saveourtool.save.utils.*
import org.slf4j.Logger
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
class UserDetailsService(
    private val userRepository: UserRepository,
    private val originalLoginRepository: OriginalLoginRepository,
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
    private val lnkUserProjectRepository: LnkUserProjectRepository,
    private val avatarStorage: AvatarStorage,
) {
    /**
     * @param user user for update
     * @return updated user
     */
    fun saveUser(user: User): User = userRepository.save(user)

    /**
     * @param username
     * @return spring's UserDetails retrieved from save's user found by provided values
     */
    fun findByName(username: String) = userRepository.findByName(username)

    /**
     * @param name
     * @return found [User] or exception
     */
    fun getByName(name: String): User = userRepository.findByName(name).orNotFound { "Not found user by name $name" }

    /**
     * @param userId [User.id]
     * @return found [User] by provided [userId] or null
     */
    fun findById(userId: Long): User? = userRepository.findByIdOrNull(userId)

    /**
     * @param username
     * @param source source (where the user identity is coming from)
     * @return spring's UserDetails retrieved from save's user found by provided values
     */
    fun findByOriginalLogin(username: String, source: String) =
            originalLoginRepository.findByNameAndSource(username, source)?.user

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
     * @param authentication
     * @return global [Role] of authenticated user
     */
    fun getGlobalRole(authentication: Authentication): Role = findById(authentication.userId())
        ?.role
        ?.let { Role.fromSpringSecurityRole(it) }
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
        // if we are registering new user (updating just name and status to NOT_APPROVED):
        return if (oldUserStatus == UserStatus.CREATED && newUser.status == UserStatus.NOT_APPROVED) {
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
     * @param source
     * @param name
     * @return existed [User] or a new one
     */
    @Transactional
    fun saveNewUserIfRequired(source: String, name: String): User =
            originalLoginRepository.findByNameAndSource(name, source)
                ?.user
                ?.also {
                    log.debug("User $name ($source) is already present in the DB")
                }
                ?: run {
                    log.info {
                        "Saving user $name ($source) with authorities $roleForNewUser to the DB"
                    }
                    saveNewUser(name).also { savedUser ->
                        addSource(savedUser, name, source)
                    }
                }

    /**
     * @param userNameCandidate
     * @return created [User]
     */
    private fun saveNewUser(userNameCandidate: String): User {
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
                role = roleForNewUser,
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

    /**
     * @param name name of user
     * @return UserSaveStatus
     */
    @Transactional
    fun banUser(
        name: String,
    ): UserSaveStatus {
        val user: User = userRepository.findByName(name).orNotFound()

        userRepository.save(user.apply {
            this.status = UserStatus.BANNED
        })

        return UserSaveStatus.BANNED
    }

    /**
     * @param name name of user
     * @return UserSaveStatus
     */
    @Transactional
    fun approveUser(
        name: String,
    ): UserSaveStatus {
        val user: User = userRepository.findByName(name).orNotFound()

        userRepository.save(user.apply {
            this.status = UserStatus.ACTIVE
        })

        return UserSaveStatus.APPROVED
    }

    companion object {
        private val log: Logger = getLogger<UserDetailsService>()
        private const val UNIQUE_NAME_SEPARATOR = "_"
        private val roleForNewUser = Role.VIEWER.asSpringSecurityRole()
    }
}
