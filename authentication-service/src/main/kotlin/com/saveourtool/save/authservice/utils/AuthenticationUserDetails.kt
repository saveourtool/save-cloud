package com.saveourtool.save.authservice.utils

import com.saveourtool.save.entities.User
import com.saveourtool.save.utils.*

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpHeaders
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.server.ServerWebExchange

/**
 * @property id [com.saveourtool.save.entities.User.id]
 * @property name [com.saveourtool.save.entities.User.name]
 * @property role [com.saveourtool.save.entities.User.role]
 */
data class AuthenticationUserDetails(
    val id: Long,
    val name: String,
    val role: String,
) : UserDetails {
    constructor(user: User) : this(user.requiredId(), user.name, user.role.orEmpty())

    /**
     * @return [PreAuthenticatedAuthenticationToken]
     */
    fun toAuthenticationToken() = PreAuthenticatedAuthenticationToken(this, NO_CREDENTIALS, authorities)

    /**
     * Populates `X-Authorization-*` headers
     *
     * @param httpHeaders
     */
    fun populateHeaders(httpHeaders: HttpHeaders) {
        httpHeaders.set(AUTHORIZATION_ID, id.toString())
        httpHeaders.set(AUTHORIZATION_NAME, name)
        httpHeaders.set(AUTHORIZATION_ROLES, role)
    }

    @JsonIgnore
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = AuthorityUtils.commaSeparatedStringToAuthorityList(role)

    @JsonIgnore
    override fun getPassword(): String = NO_CREDENTIALS

    @JsonIgnore
    override fun getUsername(): String = name

    @JsonIgnore
    override fun isAccountNonExpired(): Boolean = false

    @JsonIgnore
    override fun isAccountNonLocked(): Boolean = false

    @JsonIgnore
    override fun isCredentialsNonExpired(): Boolean = false

    @JsonIgnore
    override fun isEnabled(): Boolean = true

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<AuthenticationUserDetails>()
        private const val NO_CREDENTIALS = "N/A"

        /**
         * @return [AuthenticationUserDetails] created from values in headers
         */
        fun ServerWebExchange.toAuthenticationUserDetails(): AuthenticationUserDetails? {
            return AuthenticationUserDetails(
                id = getSingleHeader(AUTHORIZATION_ID)?.toLong() ?: return logWarnAndReturnEmpty(AUTHORIZATION_ID),
                name = getSingleHeader(AUTHORIZATION_NAME) ?: return logWarnAndReturnEmpty(AUTHORIZATION_NAME),
                role = getSingleHeader(AUTHORIZATION_ROLES) ?: return logWarnAndReturnEmpty(AUTHORIZATION_ROLES),
            )
        }

        private fun ServerWebExchange.getSingleHeader(headerName: String) = request.headers[headerName]?.singleOrNull()

        private fun <T> logWarnAndReturnEmpty(missedHeaderName: String): T? {
            log.debug {
                "Header $missedHeaderName is not provided: skipping pre-authenticated save-user authentication"
            }
            return null
        }
    }
}
