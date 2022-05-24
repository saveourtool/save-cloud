@file:Suppress("SAY_NO_TO_VAR")

package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.preprocessor.utils.RepositoryVolume
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

import java.time.Duration

@WebFluxTest(controllers = [CheckGitConnectivityController::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureWebTestClient(timeout = "60000")
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
class CheckGitConnectivityTest(
    @Autowired private var webClient: WebTestClient,
) : RepositoryVolume {
    @MockBean private lateinit var testDiscoveringService: TestDiscoveringService

    @BeforeEach
    fun webClientSetUp() {
        webClient = webClient.mutate().responseTimeout(Duration.ofSeconds(5)).build()
        whenever(testDiscoveringService.getRootTestConfig(any())).thenReturn(mock())
    }

    @Test
    fun testController() {
        var user = "test"
        var token = "test-token"
        var url = "https://github.com/saveourtool/save-cloud"
        validate(user, token, url, true)

        user = "akuleshov7"
        token = "test-token"
        url = "https://github.com/saveourtool/save-cloud111"
        validate(user, token, url, false)
    }

    private fun validate(
        user: String,
        token: String,
        url: String,
        value: Boolean,
    ) {
        webClient
            .get()
            .uri("/check-git-connectivity?user=$user&token=$token&url=$url")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ParameterizedTypeReference.forType<Boolean>(Boolean::class.java))
            .value<Nothing> {
                Assertions.assertEquals(it, value)
            }
    }

    companion object {
        @JvmStatic
        lateinit var mockServerBackend: MockWebServer

        @JvmStatic
        lateinit var mockServerOrchestrator: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServerBackend.checkQueues()
            mockServerBackend.cleanup()
            mockServerOrchestrator.checkQueues()
            mockServerOrchestrator.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServerBackend.checkQueues()
            mockServerBackend.shutdown()
            mockServerOrchestrator.checkQueues()
            mockServerOrchestrator.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerBackend = createMockWebServer()
            mockServerBackend.start()
            mockServerOrchestrator = createMockWebServer()
            mockServerOrchestrator.start()
            registry.add("save.backend") { "http://localhost:${mockServerBackend.port}" }
            registry.add("save.orchestrator") { "http://localhost:${mockServerOrchestrator.port}" }
        }
    }
}
