package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.TestStatusesService
import org.cqfn.save.entities.TestStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Suppress("MISSING_KDOC_TOP_LEVEL")
@RestController
class TestStatusesController {
    @Autowired
    private lateinit var testStatusesService: TestStatusesService

    /**
     * @param tests
     */
    @PostMapping(path = ["/saveTestStatuses"], consumes = ["application/json"])
    fun saveTestStatuses(@RequestBody tests: List<TestStatus>) {
        testStatusesService.saveTestStatuses(tests)
    }
}
