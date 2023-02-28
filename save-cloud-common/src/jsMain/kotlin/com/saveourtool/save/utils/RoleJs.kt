package com.saveourtool.save.utils

import com.saveourtool.save.domain.Role

@ExperimentalJsExport
@JsExport
object RoleJs {
    /**
     * admin in organization
     */
    val admin = Role.ADMIN

    /**
     * Has no role (synonym to null)
     */
    val none = Role.NONE

    /**
     * User that has created this project
     */
    val owner = Role.OWNER

    /**
     * Superuser, that has access to everything
     */
    val superAdmin = Role.SUPER_ADMIN

    /**
     * Has readonly access to public projects.
     */
    val viewer = Role.VIEWER

    fun invoke(role: Role) = when (role) {
        Role.ADMIN -> admin
        Role.NONE -> none
        Role.OWNER -> owner
        Role.SUPER_ADMIN -> superAdmin
        Role.VIEWER -> viewer
    }
}
