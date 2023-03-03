package com.saveourtool.save.orchestrator.utils

import com.github.dockerjava.api.model.*
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Junit extension to launch MockServer container before all tests.
 */
class MockserverExtension : BeforeAllCallback {
    private val mockserverConfigPath = "/config/initializerJson.json"

    override fun beforeAll(context: ExtensionContext?) {
        if (!isInfraStarted.getAndSet(true)) {
            val mockserverContainer = GenericContainer("mockserver/mockserver:5.15.0")
                .withExposedPorts(MOCKSERVER_SERVICE_PORT)
                .withEnv("MOCKSERVER_INITIALIZATION_JSON_PATH", mockserverConfigPath)
                .withCopyToContainer(Transferable.of(configs), mockserverConfigPath)
                .waitingFor(Wait.forListeningPort())
                .apply { start() }

            System.setProperty(MOCKSERVER_EXPOSED_PORT_PROPERTY_NAME, mockserverContainer.getMappedPort(MOCKSERVER_SERVICE_PORT).toString())
        }
    }

    companion object {
        private const val MOCKSERVER_EXPOSED_PORT_PROPERTY_NAME = "test.mockserver.url"
        const val MOCKSERVER_MOCK_URL = "/some-path-do-download-save-agent"
        private const val MOCKSERVER_SERVICE_PORT = 1080
        private val configs = """
            [
              {
                "httpRequest": {
                  "path": "$MOCKSERVER_MOCK_URL"
                },
                "httpResponse": {
                  "body": "!#/bin/bash\n echo \"sleep\"\n sleep 5000"
                }
              }
           ]
            """.trimIndent()

        @Suppress("NonBooleanPropertyPrefixedWithIs")
        private val isInfraStarted = AtomicBoolean(false)

        fun getMockserverExposedPort(): Int = System.getProperty(MOCKSERVER_EXPOSED_PORT_PROPERTY_NAME).toInt()
    }
}
