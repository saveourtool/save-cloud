package org.cqfn.save.backend.controller

import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestStatusRepository
import org.cqfn.save.backend.service.TestStatusesService
import org.cqfn.save.backend.utils.DatabaseTestBase
import org.cqfn.save.entities.TestStatus
import org.cqfn.save.teststatus.TestResultStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.time.LocalDateTime


@WebFluxTest
class TestStatusesControllerTest : DatabaseTestBase() {
    @MockBean
    lateinit var repository: ProjectRepository

    @MockBean
    lateinit var agentStatusRepository: AgentStatusRepository

    @MockBean
    lateinit var testStatusRepository: TestStatusRepository

    @MockBean
    lateinit var testStatusesService: TestStatusesService

    @Autowired
    lateinit var webClient: WebTestClient

    @Test
    fun testConnection() {
        val testStatuses = emptyList<TestStatus>()

        webClient.post()
                .uri("/saveTestStatuses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(testStatuses), List::class.java)
                .exchange()
                .expectStatus().isOk
    }

    @Test
    fun checkSavedData() {
        val testStatus = listOf(TestStatus(
                TestResultStatus.INTERNAL_ERROR,
                "666",
                LocalDateTime.now(),
                LocalDateTime.now()
            )
        )

        webClient.post()
                .uri("/saveTestStatuses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(testStatus), List::class.java)
                .exchange()
                .expectStatus().isOk

        val statuses = testStatusRepository.findAll()

        assertTrue(statuses.any { it.agentId == "666" && it.status == TestResultStatus.INTERNAL_ERROR })
    }
}