package org.cqfn.save.backend

import org.cqfn.save.backend.utils.DatabaseTestBase
import org.cqfn.save.domain.ResultStatus
import org.cqfn.save.entities.Result
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@AutoConfigureWebTestClient
class SaveResultTest(@Autowired private val webClient: WebTestClient) : DatabaseTestBase() {

    @Test
    fun checkSave() {
        webClient.post()
            .uri("/result")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(Result(1, ResultStatus.FAILED, "3.1.21"))))
            .exchange().expectBody(String::class.java)
            .isEqualTo<Nothing>("Save")
    }
}
