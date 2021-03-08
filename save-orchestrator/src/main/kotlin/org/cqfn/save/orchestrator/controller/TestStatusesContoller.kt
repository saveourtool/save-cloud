package org.cqfn.save.orchestrator.controller

import org.cqfn.save.orchestrator.service.TestStatusesService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestStatusesController {
    @Autowired
    private lateinit var testStatusesService: TestStatusesService

    // Fixme: change List<String> to Test when it will be ready
    @PostMapping("/testStatuses")
    fun receiveTestStatuses(@RequestBody tests: List<String>) {
        testStatusesService.updateTestStatuses(tests)
    }
}
