package com.saveourtool.save.demo.controller

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.runners.RunnerFactory
import com.saveourtool.save.demo.service.DemoService
import com.saveourtool.save.utils.blockingToFlux
import com.saveourtool.save.utils.blockingToMono
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
     * @return all [DemoDto]s as [Flux]
     */
    @GetMapping("/all")
    fun all(): Flux<DemoDto> = blockingToFlux {
        demoService.getAllDemos().map { it.toDto() }
    }

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
    ): Mono<DemoResult> = blockingToMono {
        demoService.findBySaveourtoolProject(organizationName, projectName)
    }
        .zipWith(demoService.getRunnerType())
        .flatMap { (demo, runnerType) ->
            demoRunnerFactory.create(demo, "manual", runnerType).run(demoRunRequest)
        }
}
