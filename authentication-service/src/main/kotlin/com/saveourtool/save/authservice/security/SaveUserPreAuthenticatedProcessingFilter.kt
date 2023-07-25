package com.saveourtool.save.authservice.security

import com.saveourtool.save.utils.AUTHORIZATION_ID
import com.saveourtool.save.utils.AUTHORIZATION_NAME
import com.saveourtool.save.utils.AUTHORIZATION_ROLES
import org.springframework.security.core.AuthenticatedPrincipal
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

@Component
class SaveUserPreAuthenticatedProcessingFilter : AbstractPreAuthenticatedProcessingFilter() {
    override fun afterPropertiesSet() {
        setAuthenticationDetailsSource { request ->
            PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(
                request,
                AuthorityUtils.commaSeparatedStringToAuthorityList(request.getHeader(AUTHORIZATION_ROLES)),
            )
        }
        super.afterPropertiesSet()
    }

    override fun getPreAuthenticatedPrincipal(request: HttpServletRequest): AuthenticatedPrincipal = SaveUserPrincipal(
        id = request.getHeader(AUTHORIZATION_ID).toLong(),
        name = request.getHeader(AUTHORIZATION_NAME),
    )

    override fun getPreAuthenticatedCredentials(request: HttpServletRequest): Any = "N/A"


}