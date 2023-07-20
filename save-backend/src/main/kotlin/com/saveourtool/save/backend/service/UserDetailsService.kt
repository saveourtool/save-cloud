package com.saveourtool.save.backend.service

import com.saveourtool.save.authservice.utils.getIdentitySourceAwareUserDetails
import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.UserSaveStatus
import com.saveourtool.save.entities.OriginalLogin
import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.orNotFound

import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import java.util.*

/**
 * A service that provides `UserDetails`
 */
@Service
class UserDetailsService(
    private val userRepository: UserRepository,
    private val originalLoginRepository: OriginalLoginRepository,
) : ReactiveUserDetailsService {
    override fun findByUsername(username: String): Mono<UserDetails> = {
        userRepository.findByName(username) ?: originalLoginRepository.findByName(username)?.user
    }.toMono().getIdentitySourceAwareUserDetails(username)

    /**
     * @param username
     * @param source source (where the user identity is coming from)
     * @return IdentitySourceAwareUserDetails retrieved from UserDetails
     */
    fun findByUsernameAndSource(username: String, source: String) =
            { originalLoginRepository.findByNameAndSource(username, source) }
                .toMono()
                .map { it.user }
                .getIdentitySourceAwareUserDetails(username, source)

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
    fun saveUser(user: User, oldName: String?): UserSaveStatus {
        val userName = user.name
        return if (oldName == null) {
            userRepository.save(user)
            UserSaveStatus.UPDATE
        } else if (userRepository.validateName(userName) != 0L) {
            userRepository.deleteHighLevelName(oldName)
            userRepository.saveHighLevelName(userName)
            userRepository.save(user)
            UserSaveStatus.UPDATE
        } else {
            UserSaveStatus.CONFLICT
        }
    }

    /**
     * @param user
     * @return UserSaveStatus
     */
    @Transactional
    fun saveNewUser(user: User) {
        val newUser = userRepository.save(user)
        originalLoginRepository.save(OriginalLogin(user.name, newUser, user.source))
    }
}
