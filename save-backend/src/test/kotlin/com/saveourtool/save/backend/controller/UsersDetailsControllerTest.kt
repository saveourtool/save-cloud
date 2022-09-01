package com.saveourtool.save.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.saveourtool.save.backend.configs.WebSecurityConfig
import com.saveourtool.save.backend.controllers.UsersDetailsController
import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.backend.utils.ConvertingAuthenticationManager
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OriginalLogin
import com.saveourtool.save.entities.User
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(controllers = [UsersDetailsController::class])
@Import(
    WebSecurityConfig::class,
    ConvertingAuthenticationManager::class,
    UserDetailsService::class,
)
@AutoConfigureWebTestClient
class UsersDetailsControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockBean
    private lateinit var userRepository: UserRepository

    @MockBean
    private lateinit var originalLoginRepository: OriginalLoginRepository

    private val objectMapper = ObjectMapper()

    @Test
    @WithMockUser
    fun `serialize and deserialize user`() {
        val user = User(
            "admin",
            null,
            Role.VIEWER.asSpringSecurityRole(),
            "basic",
            null,
            isActive = false,
        ).apply { id = 1 }

        val originalLogin = OriginalLogin(
            "admin",
            user,
            "basic"
        ).apply { id = 4 }
        user.apply { originalLogins = listOf(originalLogin) }

        val result = objectMapper.writeValueAsString(user)

        webTestClient.post()
            .uri("/internal/users/new")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(result)
            .exchange()
            .expectStatus()
            .isOk
    }
}