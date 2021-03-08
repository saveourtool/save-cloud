package org.cqfn.save.orchestrator.contoller

import org.cqfn.save.orchestrator.service.TestStatusesService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest
class TestStatusesControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @MockBean
    val testStatusesService: TestStatusesService? = null

    @Test
    fun checkPostTestStatuses() {
        val testStrings = emptyList<String>()

        webClient.post()
                .uri("/testStatuses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(testStrings), List::class.java)
                .exchange()
                .expectStatus().isOk
    }
}