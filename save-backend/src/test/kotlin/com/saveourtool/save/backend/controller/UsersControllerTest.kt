package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.repository.vulnerability.LnkVulnerabilityUserRepository
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OriginalLogin
import com.saveourtool.save.entities.User
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
@MockBeans(MockBean(LnkVulnerabilityUserRepository::class))
class UsersControllerTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    @WithMockUser
    fun `serialize and deserialize user`() {
        val user = User(
            "admin2",
            null,
            Role.VIEWER.asSpringSecurityRole(),
            "basic2",
            null,
            isActive = false,
        ).apply { id = 99 }

        val originalLogin = OriginalLogin(
            "admin",
            user,
            "basic"
        ).apply { id = 4 }
        user.apply { originalLogins = listOf(originalLogin) }

        webTestClient.post()
            .uri("/internal/users/new")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(user)
            .exchange()
            .expectStatus()
            .isOk

        webTestClient.get()
            .uri("/api/$v1/users/admin2")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<UserInfo>()
            .consumeWith {
                requireNotNull(it.responseBody)
                Assertions.assertEquals("admin2", it.responseBody!!.name)
            }
    }
}
