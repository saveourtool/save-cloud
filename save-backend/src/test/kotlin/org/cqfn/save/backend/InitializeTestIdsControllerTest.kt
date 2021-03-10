package org.cqfn.save.backend

import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.service.InitializeTestIdsService
import org.cqfn.save.backend.service.TestStatusesService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest
class InitializeTestIdsControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean
    var repository: ProjectRepository? = null

    @MockBean
    val testIdsService: InitializeTestIdsService? = null

    @MockBean
    val testStatusesService: TestStatusesService? = null

    @Test
    fun checkEndpointIsOk() {
        val testStrings = emptyList<Int>()

        webClient.post()
                .uri("/initializeTestIds")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(testStrings), List::class.java)
                .exchange()
                .expectStatus().isOk
    }
}