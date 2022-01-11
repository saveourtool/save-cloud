package org.cqfn.save.backend.utils

import org.cqfn.save.entities.User
import java.security.Principal

fun Principal.toUser(): User {
    val (identitySource, name) = name.split(':')
    return User(
        name = name,
        password = null,
        role = null,
        source = identitySource,
    )
}