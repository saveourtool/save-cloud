package com.saveourtool.save.cosv.service

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.authservice.utils.username
import com.saveourtool.save.cosv.repositorysave.*
import com.saveourtool.save.cosv.storage.AvatarStorage
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.UserSaveStatus
import com.saveourtool.save.entities.User
import com.saveourtool.save.evententities.UserEvent
import com.saveourtool.save.info.UserStatus
import com.saveourtool.save.repository.UserRepository
import com.saveourtool.save.storage.AvatarKey
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.getHighestRole
import com.saveourtool.save.utils.orNotFound
import org.springframework.context.ApplicationEventPublisher
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
class UserService(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val originalLoginRepository: OriginalLoginRepository,
    private val lnkUserOrganizationRepository: LnkUserOrganizationRepository,
    private val lnkUserProjectRepository: LnkUserProjectRepository,
    private val avatarStorage: AvatarStorage,
) {
    /**
     * @param user user for update
     * @return updated user
     */
    fun saveUser(user: User) = userRepository.save(user)

    /**
     * @param name
     * @return user with [name]
     */
    fun getUserByName(name: String): User = userRepository.findByName(name).orNotFound { "Not found user by name $name" }

    /**
     * @param id
     * @return user with [id]
     */
    fun findById(id: Long): User = userRepository.findByIdOrNull(id).orNotFound { "Not found user by id $id" }

    /**
     * @param ids
     * @return users with [ids]
     */
    fun findAllByIdIn(ids: List<Long>): List<User> = userRepository.findAllByIdIn(ids)

    /**
     * @param authentication
     * @return global [Role] of authenticated user
     */
    fun getGlobalRole(authentication: Authentication): Role = getUserByName(authentication.username())
        .role
        ?.let { Role.fromSpringSecurityRole(it) }
        ?: Role.VIEWER

    /**
     * @param userId
     * @param organizationName
     * @return role for user in organization by user ID and organization name
     */
    fun findRoleByUserIdAndOrganizationName(userId: Long, organizationName: String) = organizationRepository
        .findByUserIdAndOrganizationName(userId, organizationName)
        ?.role
        ?: Role.NONE

    /**
     * @param authentication
     * @param organizationName
     * @return the highest of two roles: the one in organization with name [organizationName] and global one.
     */
    fun getGlobalRoleOrOrganizationRole(authentication: Authentication, organizationName: String): Role {
        val selfId = authentication.userId()
        val selfGlobalRole = getGlobalRole(authentication)
        val selfOrganizationRole = findRoleByUserIdAndOrganizationName(selfId, organizationName)
        return getHighestRole(selfOrganizationRole, selfGlobalRole)
    }

    /**
     * @param newUser
     * @param oldName
     * @param oldUserStatus
     * @return UserSaveStatus
     */
    @Transactional
    fun saveUser(newUser: User, oldName: String?, oldUserStatus: UserStatus): UserSaveStatus {
        applicationEventPublisher.publishEvent(UserEvent(newUser))
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
    fun approveUser(
        name: String,
    ): UserSaveStatus {
        val user: User = userRepository.findByName(name).orNotFound()

        userRepository.save(user.apply {
            this.status = UserStatus.ACTIVE
        })

        return UserSaveStatus.APPROVED
    }

    /**
     * @param name name of user
     * @return UserSaveStatus
     */
    @Transactional
    fun banUser(
        name: String,
    ): UserSaveStatus {
        val user: User = userRepository.findByName(name).orNotFound().apply {
            this.status = UserStatus.BANNED
        }
        applicationEventPublisher.publishEvent(UserEvent(user))

        userRepository.save(user)

        return UserSaveStatus.BANNED
    }
}
