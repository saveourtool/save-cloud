package org.cqfn.save.backend.utils

import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainerProvider
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container

/**
 * A base class giving access to a database instance. Is used instead of `@Testcontainers` annotations:
 * we manually start container here, and DB connection can be used in all tests inheriting this class.
 */
open class DatabaseTestBase {
    companion object {
        @Container
        @JvmStatic
        val dbContainer: JdbcDatabaseContainer<*> = MySQLContainerProvider()
            .newInstance("8.0.20")
            .withExposedPorts(3306)
            .withDatabaseName("save_db_test")
            .waitingFor(Wait.forLogMessage("Container is started (JDBC URL: ", 1))
            .apply {
                start()
            }

        /**
         * @param registry
         */
        @DynamicPropertySource
        @JvmStatic
        fun dbProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { dbContainer.jdbcUrl }
            registry.add("spring.datasource.username") { dbContainer.username }
            registry.add("spring.datasource.password") { dbContainer.password }
        }
    }
}
