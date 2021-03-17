package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.entities.Execution
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * Controller that accepts executions
 */
@RestController
class ExecutionController(private val executionService: ExecutionService) {
    /**
     * @param execution
     */
    @PostMapping("/createExecution")
    fun createExecution(@RequestBody execution: Execution) {
        executionService.saveExecution(execution)
    }
}
