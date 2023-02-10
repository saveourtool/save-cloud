package com.saveourtool.save.demo.controller

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.runners.cli.CliRunnerFactory
import com.saveourtool.save.demo.service.DemoService
import com.saveourtool.save.demo.utils.KOTLIN_TEST_NAME
import com.saveourtool.save.utils.blockingToMono
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import kotlin.io.path.createTempDirectory

/**
 * Controller for demo
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "demo"),
)
@RestController
@RequestMapping("/demo/api/{organizationName}/{projectName}")
class DemoController(
    private val demoService: DemoService,
    private val demoCliRunnerFactory: CliRunnerFactory,
) {
    private val tmpDir = createTempDirectory("demo-")

    /**
     * @param organizationName
     * @param projectName
     * @param demoRunRequest request data class with all required additional info
     * @return [DemoResult]
     */
    @PostMapping("/run")
    fun runCheckDemo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestBody demoRunRequest: DemoRunRequest,
    ): Mono<DemoResult> = blockingToMono {
        demoService.findBySaveourtoolProject(organizationName, projectName)
    }
        .map {
            demoCliRunnerFactory.create(it, "manual").runInTempDir(
                demoRunRequest,
                tmpDir,
                testFileName = KOTLIN_TEST_NAME,
                additionalDirectoryTree = listOf("src"),
            )
        }
}
