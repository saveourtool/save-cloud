package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionUpdateDto

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controller that accepts executions
 */
@RestController
class ExecutionController(private val executionService: ExecutionService) {
    /**
     * @param execution
     * @return id of created [Execution]
     */
    @PostMapping("/createExecution")
    fun createExecution(@RequestBody execution: Execution): Long = executionService.saveExecution(execution)

    /**
     * @param executionUpdateDto
     */
    @PostMapping("/updateExecution")
    fun updateExecution(@RequestBody executionUpdateDto: ExecutionUpdateDto) {
        executionService.updateExecution(executionUpdateDto)
    }

    /**
     * @param executionId
     */
    @GetMapping("/getDto")
    fun getExecutionDto(@RequestParam executionId: Long) = executionService.getExecutionDto(executionId)
}
