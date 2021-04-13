package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.BaseEntityRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
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
    lateinit var testSuiteRepository: BaseEntityRepository<TestSuite>

    @Test
    fun testConnection() {
        val project = Project("Huawei", "huaweiName", "manual", "huaweiUrl", "description")
        val testSuite = TestSuite(
            TestSuiteType.PROJECT,
            "test",
            project,
            testLocalDateTime
        )

        webClient.post()
            .uri("/saveTestSuite")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testSuite))
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun checkDataSave() {
        val project = Project("Huawei", "huaweiName", "manual", "huaweiUrl", "description")
        val testSuite = TestSuite(
            TestSuiteType.PROJECT,
            "test",
            project,
            testLocalDateTime
        )

        webClient.post()
            .uri("/saveTestSuite")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testSuite))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = testSuiteRepository.findAll()

        assertTrue(databaseData.any { it.project?.id == testSuite.project?.id && it.name == testSuite.name })
    }
}
