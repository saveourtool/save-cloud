package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.InitializeTestIdsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class InitializeTestIdsController {
    @Autowired
    private lateinit var testIdsService: InitializeTestIdsService

    // Fixme: probably there should be a presentation of test id. Need to change
    @PostMapping("/initializeTestIds")
    fun saveTestIds(@RequestBody ids: List<Int>) {
        testIdsService.saveTestIds(ids)
    }
}
