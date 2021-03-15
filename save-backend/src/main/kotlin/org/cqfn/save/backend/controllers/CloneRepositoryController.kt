package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.Project
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@RestController
class CloneRepositoryController(private val projectService: ProjectService) {

    @PostMapping(value = ["/backTest"])
    fun saveResult(@RequestBody text: String): String {
        val project = Project("fd", "df", "df")
        projectService.addResults(project)
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
                .uri(URI.create("http://preprocessor:5200/testbackAndproc"))
                .POST(HttpRequest.BodyPublishers.ofString("heh"))
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }
}
