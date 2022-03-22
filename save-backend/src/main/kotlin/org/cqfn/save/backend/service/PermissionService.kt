package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.User
import org.cqfn.save.permission.SetRoleRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import reactor.util.function.Tuple2
import java.util.Optional

/**
 * Service for managing user permissions
 */
@Service
class PermissionService(
    private val projectService: ProjectService,
    private val userRepository: UserRepository,
    private val lnkUserProjectService: LnkUserProjectService,
) {
    /**
     * @param userName
     * @param projectName
     * @param organizationName
     */
    fun getRole(userName: String, projectName: String, organizationName: String): Mono<Role> = findUserAndProject(userName, organizationName, projectName).map { (user, project) ->
        getRole(user, project)
    }

    /**
     * @param user
     * @param project
     * @return role of [user] in [project], may throw exception if data is not consistent
     */
    @Suppress("UnsafeCallOnNullableType")
    fun getRole(user: User, project: Project): Role = lnkUserProjectService.findRoleByUserIdAndProject(user.id!!, project)

    /**
     * @param organizationName
     * @param projectName
     * @param setRoleRequest
     */
    fun addRole(organizationName: String, projectName: String, setRoleRequest: SetRoleRequest): Mono<Unit> =
            findUserAndProject(setRoleRequest.userName, organizationName, projectName)
                .map { (user: User, project: Project) ->
                    lnkUserProjectService.addRole(user, project, setRoleRequest.role)
                }

    /**
     * @param userName
     * @param organizationName
     * @param projectName
     * @return `Mono<Tuple2<User, Project>>`
     */
    @Suppress("TYPE_ALIAS")
    internal fun findUserAndProject(userName: String,
                                    organizationName: String,
                                    projectName: String
    ): Mono<Tuple2<User, Project>> = Mono.zip(
        Mono.justOrEmpty(userRepository.findByName(userName)),
        Mono.justOrEmpty(
            Optional.ofNullable(
                projectService.findByNameAndOrganizationName(projectName, organizationName)
            )
        ),
    )
}
