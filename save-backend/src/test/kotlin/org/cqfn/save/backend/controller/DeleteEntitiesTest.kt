package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.controllers.ProjectController
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.utils.MySqlExtension
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
    MockBean(ProjectController::class),
    MockBean(ProjectPermissionEvaluator::class),
)
class DeleteEntitiesTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @Test
    @Disabled("should rollback after committing in the db")
    fun testDeleteExecutionById() {
        val ids = listOf(1L, 2L, 3L).joinToString(",")
        webClient.post()
            .uri("/api/execution/delete?executionIds=$ids")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    @Disabled("should rollback after committing in the db")
    fun testDeleteExecution() {
        webClient.post()
            .uri("/api/execution/deleteAll?name=huaweiName&owner=Huawei")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
    }
}
