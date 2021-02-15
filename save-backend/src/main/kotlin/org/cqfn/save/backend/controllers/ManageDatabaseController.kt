package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.services.ManageDatabaseService
import org.springframework.web.bind.annotation.*

@RestController("/database")
class ManageDatabaseController(private val databaseService: ManageDatabaseService) {

    /**
     * Saves all tests of agent
     */
    @PostMapping("/saveTests")
    fun saveReadyTests(@RequestBody tests: List<String>) {
        databaseService.saveTests(tests)
    }

    /**
     * This returns back tests that are ready
     * Maybe it is not needed.
     *
     * @param id Is it needed in here? It can be agents id
     */
    @GetMapping("/getReadyTests")
    fun getReadyTests(@RequestParam id: String) {
        databaseService.getReadyTests(id)
    }
}