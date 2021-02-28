package org.cqfn.save.backend

import com.google.gson.Gson
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
class SaveResultTest(@Autowired private val webClient: WebTestClient) {

    @Test
    fun checkSave() {
        val body = Gson().toJson(listOf(Result(1, "run", "3.1.21")))
        webClient.post()
            .uri("/result")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(body))
            .exchange().expectBody(String::class.java)
            .isEqualTo<Nothing>("Save")
    }
}
