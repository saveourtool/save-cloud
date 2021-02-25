package org.cqfn.save.preprocessor.controllers

import org.cqfn.save.entities.service.TestService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.eclipse.jgit.api.Git
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ComponentScan
import org.springframework.web.bind.annotation.RequestParam
import java.io.File

/**
 * A Spring controller for git project downloading
 */
@RestController
class DownloadProject(@Autowired private val testService: TestService) {

    @PostMapping(value = ["/upload"])
    fun upload(@RequestParam("url") url: String) {

        val tempDir = File("../dir")
        Git.cloneRepository()
            .setURI(url)
            .setDirectory(tempDir)
            .call().use {
                println("cloned")
            }

    }
}