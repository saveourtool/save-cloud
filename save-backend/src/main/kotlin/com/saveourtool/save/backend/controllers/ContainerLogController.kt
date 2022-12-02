package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.AgentService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.service.LogService
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.toInstantAtDefaultZone
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * Controller to fetch logs
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "logs"),
)
@RestController
@RequestMapping(path = ["/api/$v1/logs"])
class ContainerLogController(
    private val logService: LogService,
    private val agentService: AgentService,
) {
    @GetMapping("/by-container-name")
    @Operation(
        method = "GET",
        summary = "Get logs for containerName in provided time range.",
        description = "Get logs for containerName in provided time range.",
    )
    @Parameters(
        Parameter(name = "containerName", `in` = ParameterIn.QUERY, description = "name of a container", required = true),
        Parameter(name = "from", `in` = ParameterIn.QUERY, description = "start of requested time range in ISO format with default time zone", required = true),
        Parameter(name = "to", `in` = ParameterIn.QUERY, description = "end of requested time range in ISO format with default time zone", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched logs for container.")
    fun getByContainerName(
        @RequestParam containerName: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime,
    ): Mono<StringListResponse> = logService.getByContainerName(containerName, from.toInstantAtDefaultZone(), to.toInstantAtDefaultZone())
        .map { ResponseEntity.ok(it) }

    @GetMapping("/from-agent")
    @Operation(
        method = "GET",
        summary = "Get all logs from agent by container name.",
        description = "Get all logs from agent by container name.",
    )
    @Parameters(
        Parameter(name = "containerName", `in` = ParameterIn.QUERY, description = "name of a container", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched logs for container.")
    fun logs(
        @RequestParam containerName: String,
    ): Mono<StringListResponse> = blockingToMono {
        agentService.getAgentByContainerName(containerName)
    }
        .map {
            agentService.getAgentTimes(it)
        }
        .flatMap { (from, to) ->
            logService.getByContainerName(
                containerName,
                from.toInstantAtDefaultZone(),
                to.toInstantAtDefaultZone()
            )
        }
        .map { ResponseEntity.ok(it) }
}
