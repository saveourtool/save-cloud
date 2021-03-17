package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.TestInitializeService
import org.cqfn.save.entities.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class TestInitializeController {
    @Autowired
    private lateinit var testInitializeService: TestInitializeService

    @PostMapping("/initializeTests")
    fun initializeTests(@RequestBody tests: List<Test>) {
        testInitializeService.saveTests(tests)
    }
}