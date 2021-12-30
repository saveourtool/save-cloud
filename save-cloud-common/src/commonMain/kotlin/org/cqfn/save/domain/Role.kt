package org.cqfn.save.domain

enum class Role {
    ADMIN,
    PROJECT_OWNER,
    PROJECT_ADMIN,
    VIEWER,
    ;

    fun asSpringSecurityRole() = "ROLE_$name"
}
