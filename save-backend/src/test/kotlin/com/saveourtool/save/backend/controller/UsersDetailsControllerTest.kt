package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.v1
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(InfraExtension::class)
class UsersDetailsControllerTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    @WithMockUser(value = "JohnDoe", roles = ["SUPER_ADMIN"])
    fun `update other user`() {
        mutateMockedUser(id = 2)

        val newUserInfo = UserInfo(
            name = "admin",
            email = "example@save.com",
            company = "Example",
        )

        webClient.post()
            .uri("/api/$v1/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @WithMockUser(value = "user", roles = ["SUPER_ADMIN"])
    fun `new user registration with a free name`() {
        mutateMockedUser(id = 3)

        val newUserInfo = UserInfo(
            name = "Kuleshov",
            oldName = "user",
            email = "example@save.com",
            company = "Example",
        )

        webClient.post()
            .uri("/api/$v1/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
    }

    @Test
    @WithMockUser(value = "JohnDoe", roles = ["SUPER_ADMIN"])
    fun `new user registration with a taken name admin`() {
        mutateMockedUser(id = 2)

        val newUserInfo = UserInfo(
            name = "admin",
            email = "example@save.com",
            company = "Example",
        )

        webClient.post()
            .uri("/api/$v1/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `update not a name but other info`() {
        mutateMockedUser(id = 1)

        val newUserInfo = UserInfo(
            id = 1,
            name = "admin",
            oldName = null,
            email = "example@save.com",
            company = "Example Company",
        )

        webClient.post()
            .uri("/api/$v1/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.OK)
    }

    @Test
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `update existing user with the same name`() {
        mutateMockedUser(id = 1)

        val newUserInfo = UserInfo(
            name = "admin",
            email = "example@save.com",
            company = "Example",
            oldName = "admin"
        )

        webClient.post()
            .uri("/api/$v1/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `update user with taken name`() {
        mutateMockedUser(id = 1)

        val newUserInfo = UserInfo(
            name = "JohnDoe",
            oldName = "admin",
            email = "example@save.com",
            company = "Example",
        )

        webClient.post()
            .uri("/api/$v1/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
    }
}
