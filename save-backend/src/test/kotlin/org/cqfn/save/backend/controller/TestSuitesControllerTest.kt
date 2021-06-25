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

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class TestSuitesControllerTest {
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
                val body = it.responseBody!!
                Assert.assertEquals(listOf(testSuite).size, body.size)
                Assert.assertEquals(testSuite.name, body[0].name)
                Assert.assertEquals(testSuite.project, body[0].project)
                Assert.assertEquals(testSuite.type, body[0].type)
            }
    }

    @Test
    fun `saved test suites should be persisted in the DB`() {
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
