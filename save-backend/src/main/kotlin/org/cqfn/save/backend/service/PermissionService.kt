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

@Service
class PermissionService(
    private val projectService: ProjectService,
    private val userRepository: UserRepository,
    private val lnkUserProjectService: LnkUserProjectService,
) {
    fun getRole(userName: String, projectName: String, organizationName: String): Mono<Role> {
        return findUserAndProject(userName, organizationName, projectName).map { (user, project) ->
            getRole(user, project)
        }
    }

    fun getRole(user: User, project: Project): Role {
        return lnkUserProjectService.findRoleByUserIdAndProject(user.id!!, project)
    }

    fun addRole(organizationName: String, projectName: String, setRoleRequest: SetRoleRequest): Mono<Unit> =
            findUserAndProject(setRoleRequest.userName, organizationName, projectName)
                .map { (user: User, project: Project) ->
                    lnkUserProjectService.addRole(user, project, setRoleRequest.role)
                }

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
