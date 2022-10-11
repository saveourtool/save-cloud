package com.saveourtool.save.sandbox.utils

import com.saveourtool.save.utils.AuthenticationDetails
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication

/**
 * @return userId
 */
fun Authentication.userId() = (this.details as AuthenticationDetails).id

/**
 * @return username
 */
fun Authentication.userName() = this.extractUserNameAndIdentitySource().first

/**
 * @return identitySource
 */
fun Authentication.identitySource() = this.extractUserNameAndIdentitySource().second

/**
 * @return
 * @throws BadCredentialsException
 */
fun Authentication.extractUserNameAndIdentitySource(): Pair<String, String> {
    val identitySource = (this.details as AuthenticationDetails).identitySource
    if (identitySource == null || !this.name.startsWith("$identitySource:")) {
        throw BadCredentialsException(this.name)
    }
    val name = this.name.drop(identitySource.length + 1)
    return name to identitySource
}
