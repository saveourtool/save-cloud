package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType
import org.junit.Assert
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime
import java.time.Month

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class TestSuitesControllerTest {
    private val testLocalDateTime = LocalDateTime.of(2020, Month.APRIL, 10, 16, 30, 20)

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var testSuiteRepository: TestSuiteRepository

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Test
    fun `should accept test suites and return saved test suites`() {
        val project = projectRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            TestSuiteType.PROJECT,
            "test",
            project,
            "save.properties"
        )

        webClient.post()
            .uri("/saveTestSuites")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testSuite)))
            .exchange()
            .expectBody<List<TestSuite>>()
            .consumeWith {
                val body = it.responseBody
                Assert.assertEquals(body.size, listOf(testSuite).size)
                Assert.assertEquals(body[0].name, testSuite.name)
                Assert.assertEquals(body[0].project, testSuite.project)
                Assert.assertEquals(body[0].type, testSuite.type)
            }
    }

    @Test
    fun checkDataSave() {
        val project = projectRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            TestSuiteType.PROJECT,
            "test",
            project,
            "save.properties"
        )

        webClient.post()
            .uri("/saveTestSuites")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(testSuite)))
            .exchange()
            .expectBody<List<TestSuite>>()

        val databaseData = testSuiteRepository.findAll()

        assertTrue(databaseData.any { it.project?.id == testSuite.project?.id && it.name == testSuite.name })
    }
}
