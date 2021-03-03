package org.cqfn.save.preprocessor

import org.cqfn.save.repository.GitRepository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import java.io.File

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [SaveApplication::class],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DownloadProjectTest {

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @Test
    fun testBadRequest() {
        val request = HttpEntity(GitRepository("wrongRepo"))
        val result: ResponseEntity<String> = testRestTemplate
                .postForEntity("/upload", request, GitRepository::class)
        assertEquals(result.statusCode, HttpStatus.BAD_REQUEST)
    }

    @Test
    fun testCorrectDownload() {
        val url = "https://github.com/cqfn/save-cloud"
        val request = HttpEntity(GitRepository(url))
        val result: ResponseEntity<String> = testRestTemplate
                .postForEntity("/upload", request, GitRepository::class)
        assertEquals(result.statusCode, HttpStatus.ACCEPTED)
        assertTrue(File("/home/repositories/${url.hashCode()}").exists())
    }
}
