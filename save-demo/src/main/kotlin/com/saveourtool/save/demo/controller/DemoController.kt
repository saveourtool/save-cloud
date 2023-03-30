package com.saveourtool.save.demo.controller

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.runners.RunnerFactory
import com.saveourtool.save.demo.service.DemoService
import com.saveourtool.save.filters.DemoFilter
import com.saveourtool.save.utils.blockingToFlux
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Controller for demo
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "demo"),
)
@RestController
@RequestMapping("/demo/api")
class DemoController(
    private val demoService: DemoService,
    private val demoRunnerFactory: RunnerFactory,
) {
    /**
     * @param filter [DemoFilter], [DemoFilter.any] by default
     * @param demoAmount number of [DemoDto]s that should be fetched, [DemoDto.DEFAULT_FETCH_NUMBER] by default
     * @return all [DemoDto]s matching [filter] as [Flux]
     */
    @PostMapping("/demo-list")
    fun getFilteredDemoList(
        @RequestBody(required = false) filter: DemoFilter = DemoFilter.any,
        @RequestParam(required = false, defaultValue = DemoDto.DEFAULT_FETCH_NUMBER.toString())
        demoAmount: Int = DemoDto.DEFAULT_FETCH_NUMBER,
    ): Flux<DemoDto> = blockingToFlux {
        demoService.getFiltered(filter, demoAmount).toList().map { it to demoService.getStatus(it).block() }
    }
        .filter { (_, status) -> status in filter.statuses }
        .map { it.first.toDto() }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param demoRunRequest request data class with all required additional info
     * @return tool output as [DemoResult]
     */
    @PostMapping("/{organizationName}/{projectName}/run")
    fun runDemo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestBody demoRunRequest: DemoRunRequest,
    ): Mono<DemoResult> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .zipWith(demoService.getRunnerType())
        .flatMap { (demo, runnerType) ->
            demoRunnerFactory.create(demo, "manual", runnerType).run(demoRunRequest)
        }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @return list of available modes to run the demo with
     */
    @GetMapping("/{organizationName}/{projectName}/modes")
    fun getModes(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Flux<String> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMapIterable { demo -> demo.runCommands.map { runCommand -> runCommand.command } }
}
