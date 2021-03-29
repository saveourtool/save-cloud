package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.Project

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class ProjectControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @Test
    fun `should return all projects`() {
        webClient
            .get()
            .uri("/projects")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ParameterizedTypeReference.forType<List<Project>>(List::class.java))
            .value<Nothing> {
                Assertions.assertTrue(it.isNotEmpty())
            }
    }
}
