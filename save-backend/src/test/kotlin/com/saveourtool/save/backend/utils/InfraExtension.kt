package com.saveourtool.save.backend.utils

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.MySQLContainerProvider
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Junit extension to launch MySql and S3 (minio) containers before all tests.
 */
class InfraExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext?) {
        if (!isInfraStarted.getAndSet(true)) {
            val dbContainer: JdbcDatabaseContainer<*> = MySQLContainerProvider()
                .newInstance("8.0.28-oracle")
                .withExposedPorts(MySQLContainer.MYSQL_PORT)
                .withDatabaseName("save_db_test")
                .waitingFor(Wait.forLogMessage("Container is started (JDBC URL: ", 1))
                .apply {
                    start()
                }

            val minioContainer = GenericContainer("minio/minio:latest")
                .withCommand("server /data")
                .withExposedPorts(MINIO_SERVICE_PORT)
                .withEnv("MINIO_ROOT_USER", MINIO_ROOT_USER)
                .withEnv("MINIO_ROOT_PASSWORD", MINIO_ROOT_PASSWORD)
                .waitingFor(Wait.forListeningPort())
                .apply { start() }
            GenericContainer("minio/mc:latest")
                .dependsOn(minioContainer)
                .withCreateContainerCmdModifier { cmd ->
                    cmd.withEntrypoint(
                        "/bin/sh",
                        "-c",
                        sequenceOf(
                            "alias set minio ${minioContainer.getS3UrlInDocker()} $MINIO_ROOT_USER $MINIO_ROOT_PASSWORD",
                            "mb --ignore-existing minio/$MINIO_BUCKET_NAME",
                            "policy set public minio/$MINIO_BUCKET_NAME",
                        )
                            .joinToString(" && ") { "/usr/bin/mc $it" }
                    )
                }
                .waitingFor(Wait.forLogMessage("Bucket created successfully.*$MINIO_BUCKET_NAME.*", 1))
                .apply { start() }

            System.setProperty("backend.s3-storage.endpoint", minioContainer.getS3Url())
            System.setProperty("backend.s3-storage.bucketName", MINIO_BUCKET_NAME)
            System.setProperty("backend.s3-storage.credentials.accessKeyId", MINIO_ROOT_USER)
            System.setProperty("backend.s3-storage.credentials.secretAccessKey", MINIO_ROOT_PASSWORD)
            System.setProperty("spring.datasource.url", dbContainer.jdbcUrl)
            System.setProperty("spring.datasource.username", dbContainer.username)
            System.setProperty("spring.datasource.password", dbContainer.password)
        }
    }

    companion object {
        private const val MINIO_BUCKET_NAME = "cnb-test"
        private const val MINIO_ROOT_USER = "admin"
        private val MINIO_ROOT_PASSWORD = RandomStringUtils.randomAlphanumeric(8)
        private const val MINIO_SERVICE_PORT = 9000

        @Suppress("NonBooleanPropertyPrefixedWithIs")
        private val isInfraStarted = AtomicBoolean(false)

        @Suppress("HttpUrlsUsage")
        private fun GenericContainer<*>.getS3UrlInDocker() = "http://host.docker.internal:${getMappedPort(MINIO_SERVICE_PORT)}"

        @Suppress("HttpUrlsUsage")
        private fun GenericContainer<*>.getS3Url() = "http://$host:${getMappedPort(MINIO_SERVICE_PORT)}"
    }
}
