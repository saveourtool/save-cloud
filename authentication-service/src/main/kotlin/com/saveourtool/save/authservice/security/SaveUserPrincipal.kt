package com.saveourtool.save.authservice.security

import com.saveourtool.save.authservice.security.SaveUserPrincipal.Companion.getSingleHeader
import com.saveourtool.save.utils.*
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.server.ServerWebExchange

/**
 * @param id
 * @param name
 * @param authorities
 */
data class SaveUserPrincipal(
    val id: Long,
    val name: String,
    val authorities: Collection<GrantedAuthority>,
) : AuthenticatedPrincipal {
    override fun getName(): String = this.name

    /**
     * @return [PreAuthenticatedAuthenticationToken]
     */
    fun toAuthenticationToken() = PreAuthenticatedAuthenticationToken(this, "N/A", authorities)

    companion object {
        private val log = getLogger<SaveUserPrincipal>()

        /**
         * @return [SaveUserPrincipal] created from values in headers
         */
        fun ServerWebExchange.toSaveUserPrincipal(): SaveUserPrincipal? {
            return SaveUserPrincipal(
                id = getSingleHeader(AUTHORIZATION_ID)?.toLong() ?: return logWarnAndReturnEmpty(AUTHORIZATION_ID),
                name = getSingleHeader(AUTHORIZATION_NAME) ?: return logWarnAndReturnEmpty(AUTHORIZATION_NAME),
                authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(
                    getSingleHeader(AUTHORIZATION_ROLES) ?: return logWarnAndReturnEmpty(AUTHORIZATION_ROLES),
                ),
            )
        }

        private fun ServerWebExchange.getSingleHeader(headerName: String) = request.headers[headerName]?.singleOrNull()

        private fun <T> logWarnAndReturnEmpty(missedHeaderName: String): T? {
            log.warn {
                "Header $missedHeaderName is not provided: skipping pre-authenticated save-user authentication"
            }
            return null
        }
    }
}
