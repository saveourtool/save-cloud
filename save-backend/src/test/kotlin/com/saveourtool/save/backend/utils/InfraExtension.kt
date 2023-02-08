package com.saveourtool.save.backend.utils

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.MySQLContainerProvider
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Junit extension to launch MySql and S3 (minio) containers before all tests.
 */
class InfraExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext?) {
        if (!isInfraStrated.getAndSet(true)) {
            val dbContainer: JdbcDatabaseContainer<*> = MySQLContainerProvider()
                .newInstance("8.0.28-oracle")
                .withExposedPorts(MySQLContainer.MYSQL_PORT)
                .withDatabaseName("save_db_test")
                .waitingFor(Wait.forLogMessage("Container is started (JDBC URL: ", 1))
                .apply {
                    start()
                }

            val minioContainer = DockerComposeContainer(File(PATH_TO_DOCKER_COMPOSE_FILE))
                .withExposedService(MINIO_SERVICE_NAME, MINIO_SERVICE_PORT, Wait.forListeningPort())
                .apply {
                    start()
                }
            System.setProperty("backend.s3-storage.endpoint", minioContainer.getServiceUrl(MINIO_SERVICE_NAME, MINIO_SERVICE_PORT))
            System.setProperty("spring.datasource.url", dbContainer.jdbcUrl)
            System.setProperty("spring.datasource.username", dbContainer.username)
            System.setProperty("spring.datasource.password", dbContainer.password)
        }
    }

    companion object {
        private const val PATH_TO_DOCKER_COMPOSE_FILE = "src/test/resources/docker-compose.yaml"
        private const val MINIO_SERVICE_NAME = "minio"
        private const val MINIO_SERVICE_PORT = 9090

        @Suppress("NonBooleanPropertyPrefixedWithIs")
        private val isInfraStrated = AtomicBoolean(false)

        private fun DockerComposeContainer<*>.getServiceUrl(
            serviceName: String,
            servicePort: Int,
        ) ="${getServiceHost(serviceName, servicePort)}:${getServicePort(serviceName, servicePort)}"
    }
}
