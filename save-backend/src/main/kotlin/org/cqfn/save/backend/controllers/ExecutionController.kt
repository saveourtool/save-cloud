package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.ExecutionService
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionUpdateDto

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
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
     * @return execution dto
     */
    @GetMapping("/executionDto")
    fun getExecutionDto(@RequestParam executionId: Long): ResponseEntity<ExecutionDto> =
            executionService.getExecutionDto(executionId)?.let {
                ResponseEntity.status(HttpStatus.OK).body(it)
            } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    /**
     * @param project
     * @return list of execution dtos
     */
    @GetMapping("/executionDtoByNameAndOwner")
    @Transactional
    fun getExecutionByProject(@RequestParam name: String, @RequestParam owner: String): ExecutionDtoListResponse =
            executionService.getExecutionDtoByNameAndOwner(name, owner)?.let {
                ResponseEntity.status(HttpStatus.OK).body(it)
            } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()
}

typealias ExecutionDtoListResponse = ResponseEntity<List<ExecutionDto>>
