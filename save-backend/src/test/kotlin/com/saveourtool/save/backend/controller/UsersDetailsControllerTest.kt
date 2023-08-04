package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.utils.InfraExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.info.UserStatus
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
    @WithMockUser(value = "admin", roles = ["SUPER_ADMIN"])
    fun `update user`() {
        mutateMockedUser(id = 1)

        val newUserInfo = UserInfo(
            name = "admin",
            email = "example@save.com",
            company = "Example",
            status = UserStatus.ACTIVE,
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
    fun `update user invalid name`() {
        mutateMockedUser(id = 1)

        val newUserInfo = UserInfo(
            name = "JohnDoe",
            oldName = "admin",
            email = "example@save.com",
            company = "Example",
            status = UserStatus.ACTIVE,
        )

        webClient.post()
            .uri("/api/$v1/users/save")
            .bodyValue(newUserInfo)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.CONFLICT)
    }
}
