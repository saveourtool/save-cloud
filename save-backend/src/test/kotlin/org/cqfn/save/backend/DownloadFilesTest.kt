package org.cqfn.save.backend

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

import kotlin.io.path.ExperimentalPathApi

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@AutoConfigureWebTestClient
class DownloadFilesTest(@Autowired private val webClient: WebTestClient) {

    @Test
    fun checkDownload() {
        webClient.get().uri("/download").exchange()
            .expectStatus().isOk
        webClient.get().uri("/download").exchange()
            .expectBody(String::class.java).isEqualTo<Nothing>("qweqwe")
    }

    @Test
    @ExperimentalPathApi
    fun checkUpload() {
        val tmpFile = kotlin.io.path.createTempFile("test", "txt")

        val body = MultipartBodyBuilder().apply {
            part("file", object : ByteArrayResource("testString".toByteArray()) {
                override fun getFilename() = tmpFile.fileName.toString()
            })
        }.build()

        webClient.post().uri("/upload").body(BodyInserters.fromMultipartData(body))
            .exchange().expectStatus().isOk

        webClient.post().uri("/upload").body(BodyInserters.fromMultipartData(body))
            .exchange().expectBody(String::class.java).isEqualTo<Nothing>("test")
    }
}
