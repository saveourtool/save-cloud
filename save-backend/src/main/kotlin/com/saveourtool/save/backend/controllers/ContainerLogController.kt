package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.AgentService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.service.LogService
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.Instant
import java.time.ZoneId

typealias StringFluxResponse = ResponseEntity<Flux<String>>

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
        Parameter(name = "from", `in` = ParameterIn.QUERY, description = "start of requested time range", required = true),
        Parameter(name = "to", `in` = ParameterIn.QUERY, description = "end of requested time range", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched logs for container.")
    fun getByContainerName(
        @RequestParam containerName: String,
        @RequestParam from: Instant,
        @RequestParam to: Instant,
    ): StringFluxResponse = ResponseEntity.ok(logService.get(containerName, from, to))

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
    ): StringFluxResponse = blockingToMono {
        agentService.getAgentByContainerName(containerName)
    }
        .map {
            agentService.getAgentTimes(it)
        }
        .flatMapMany { (from, to) ->
            logService.get(
                containerName,
                from.atZone(ZoneId.systemDefault()).toInstant(),
                to.atZone(ZoneId.systemDefault()).toInstant()
            )
        }
        .let {
            ResponseEntity.ok(it)
        }
}
