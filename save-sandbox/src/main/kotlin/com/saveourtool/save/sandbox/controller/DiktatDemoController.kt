package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.demo.diktat.DiktatDemoMode
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.sandbox.service.DiktatDemoService
import com.saveourtool.save.utils.*

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller for diktat-demo
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "diktat-demo"),
)
@RestController
@RequestMapping("/api/diktat-demo")
class DiktatDemoController(
    private val diktatDemoService: DiktatDemoService,
) {
    /**
     * @param inputFiles
     * @param tool
     * @param mode
     * @return [DiktatDemoResult]
     */
    @PostMapping("/run")
    fun runCheckDemo(
        @RequestBody inputFiles: Pair<String, String>,
        @RequestParam(required = false, defaultValue = "DIKTAT") tool: DiktatDemoTool,
        @RequestParam(required = false, defaultValue = "FIX") mode: DiktatDemoMode,
    ): Mono<DiktatDemoResult> = blockingToMono {
        diktatDemoService.runDemo(inputFiles.first, listOf(tool.toString(), mode.toString(), inputFiles.second))
    }
}
