package com.saveourtool.save.demo.controller

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.demo.diktat.*
import com.saveourtool.save.demo.service.DiktatDemoService
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
@RequestMapping("/demo/api/diktat")
class DiktatDemoController(
    private val diktatDemoService: DiktatDemoService,
) {
    /**
     * @param diktatRunRequest request data class with all required additional info
     * @return [DiktatDemoResult]
     */
    @PostMapping("/run")
    fun runCheckDemo(
        @RequestBody diktatRunRequest: DiktatRunRequest,
    ): Mono<DiktatDemoResult> = blockingToMono {
        diktatDemoService.launch(diktatRunRequest.codeLines, diktatRunRequest.params)
    }
}
