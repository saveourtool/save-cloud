package org.cqfn.save.backend

import org.cqfn.save.backend.configs.WebSecurityConfig
import org.cqfn.save.backend.controllers.ProjectController
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.GitRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.service.UserDetailsService
import org.cqfn.save.backend.utils.ConvertingAuthenticationManager
import org.cqfn.save.entities.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.Base64
import java.util.Optional

@WebFluxTest(controllers = [ProjectController::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(
    UserDetailsService::class,
    ConvertingAuthenticationManager::class,
    WebSecurityConfig::class
)
@ActiveProfiles("secure")
@AutoConfigureWebTestClient
@MockBeans(
    MockBean(ProjectService::class),
    MockBean(GitService::class),
    MockBean(AgentRepository::class),
    MockBean(AgentStatusRepository::class),
    MockBean(ExecutionRepository::class),
    MockBean(ProjectRepository::class),
    MockBean(TestExecutionRepository::class),
    MockBean(TestRepository::class),
    MockBean(TestSuiteRepository::class),
    MockBean(GitRepository::class),
    MockBean(StandardSuitesUpdateScheduler::class),
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BasicSecurityTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient
    @MockBean private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        whenever(userRepository.findByName("user")).thenReturn(
            Optional.of(User("user", null, "ROLE_USER", "basic"))
        )
    }

    @Test
    fun `should allow access for registered user`() {
        webTestClient.get()
            .uri("/api/projects")
            .header(HttpHeaders.AUTHORIZATION, "Basic ${"user:".base64Encode()}")
            .header("X-Authorization-Source", "basic")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `should forbid requests if user has the same name but different source`() {
        webTestClient.get()
            .uri("/api/projects")
            .header(HttpHeaders.AUTHORIZATION, "Basic ${"user:".base64Encode()}")
            .header("X-Authorization-Source", "github")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `should forbid requests if user has the same name but no source`() {
        webTestClient.get()
            .uri("/api/projects")
            .header(HttpHeaders.AUTHORIZATION, "Basic ${"user:".base64Encode()}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}

private fun String.base64Encode() = Base64.getEncoder().encodeToString(toByteArray())
