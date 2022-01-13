package org.cqfn.save.domain

enum class Role {
    /**
     * Superuser, that has access to everything
     */
    ADMIN,

    /**
     * User that created a project. Can modify and delete the project and invite admins.
     */
    PROJECT_OWNER,

    /**
     * Has write access to project, can start executions.
     */
    PROJECT_ADMIN,

    /**
     * Has readonly access to public projects.
     */
    VIEWER,
    ;

    fun asSpringSecurityRole() = "ROLE_$name"
}
