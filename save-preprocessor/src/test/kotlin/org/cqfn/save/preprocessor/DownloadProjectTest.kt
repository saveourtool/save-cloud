package org.cqfn.save.preprocessor

import org.cqfn.save.preprocessor.utils.RepositoryVolume
import org.cqfn.save.repository.GitRepository

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.io.File
import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DownloadProjectTest(@Autowired private val webClient: WebTestClient) : RepositoryVolume() {

    @Value("\${save.repository}")
    private lateinit var volumes: String

    @Test
    fun testBadRequest() {
        val wrongRepo = GitRepository("wrongRepo")
        webClient.post()
            .uri("/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(wrongRepo))
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun testCorrectDownload() {
        val wrongRepo = GitRepository("https://github.com/cqfn/save-cloud.git")
        webClient.post()
            .uri("/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(wrongRepo))
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody(String::class.java)
            .isEqualTo<Nothing>("Cloned")
        Assertions.assertTrue(File("$volumes/${wrongRepo.url.hashCode()}").exists())
    }

    @AfterEach
    fun removeTestDir() {
        File(volumes).deleteRecursively()
    }
}
