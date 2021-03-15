package org.cqfn.save.orchestrator.controller

import org.cqfn.save.entities.TestStatus
import org.cqfn.save.orchestrator.service.TestStatusesService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for test statuses
 */
@RestController
class TestStatusesController {
    @Autowired
    private lateinit var testStatusesService: TestStatusesService

    /**
     * @param tests
     */
    @PostMapping("/testStatuses")
    fun receiveTestStatuses(@RequestBody tests: List<TestStatus>) {
        testStatusesService.updateTestStatuses(tests)
    }
}
