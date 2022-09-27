package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.configs.ApiSwaggerSupport
import com.saveourtool.save.backend.service.TestSuitesService
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.blockingToMono

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

/**
 * Controller for test suites
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "test-suites"),
    Tag(name = "internal"),
)
@RestController
@RequestMapping("/internal/test-suites")
class TestSuitesController(
    private val testSuitesService: TestSuitesService,
) {
    @PostMapping("/save")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Save new test suite into DB.",
        description = "Save new test suite into DB.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved test suites.")
    fun saveTestSuite(@RequestBody testSuiteDto: TestSuiteDto): Mono<TestSuite> =
            blockingToMono {
                testSuitesService.saveTestSuite(testSuiteDto)
            }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get test suite by id.",
        description = "Get test suite by id.",
    )
    @Parameters(
        Parameter(name = "id", `in` = ParameterIn.PATH, description = "id of test suite", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched filtered test suites.")
    fun getTestSuiteById(@PathVariable id: Long): ResponseEntity<TestSuite?> =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.findTestSuiteById(id))

    @PostMapping("/delete")
    @Transactional
    @Operation(
        method = "POST",
        summary = "Delete test suites.",
        description = "Delete test suites.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted test suites.")
    fun deleteTestSuite(@RequestBody testSuiteDtos: List<TestSuiteDto>): ResponseEntity<Unit> =
            ResponseEntity.status(HttpStatus.OK).body(testSuitesService.deleteTestSuiteDto(testSuiteDtos))
}
