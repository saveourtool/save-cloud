package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.IdResponse
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.ExecutionService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.utils.username
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.ExecutionRunRequest
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.permission.Permission
import jdk.nashorn.internal.runtime.regexp.joni.Config.log
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/run")
class RunExecutionController(
    private val projectService: ProjectService,
    private val executionService: ExecutionService,
    private val configProperties: ConfigProperties,
) {

    fun triggerByContestId(
        @RequestBody request: ExecutionRunRequest,
        authentication: Authentication,
    ): Mono<IdResponse> = with(request.project) {
            // Project cannot be taken from executionRequest directly for permission evaluation:
            // it can be fudged by user, who submits it. We should get project from DB based on name/owner combination.
            projectService.findWithPermissionByNameAndOrganization(authentication, name, organizationName, Permission.WRITE)
        }.flatMap { project ->
                val newExecution = createNewExecution(
                    project,
                    authentication.username(),
                    // FIXME: remove this type
                    ExecutionType.GIT,
                    configProperties.initialBatchSize,
                    request.sdk
                )

                val projectCoordinates = ProjectCoordinates(project.organization.name, project.name)
                sendToPreprocessor(
                    executionRequest,
                    ExecutionType.GIT,
                    authentication.username(),
                    fileStorage.convertToLatestFileInfo(projectCoordinates, files)
                ) { executionRequest, savedExecution ->
                    executionRequest.copy(executionId = savedExecution.requiredId())
                }
            }


    private fun createNewExecution(
        project: Project,
        username: String,
        type: ExecutionType,
        batchSize: Int,
        sdk: Sdk,
        additionalFiles: String,
    ): Execution {
        val execution = Execution.stub(project).apply {
            status = ExecutionStatus.PENDING
            this.batchSize = batchSize
            this.sdk = sdk.toString()
            this.type = type
            id = executionService.saveExecutionAndReturnId(this, username)
        }
        return execution
    }
}