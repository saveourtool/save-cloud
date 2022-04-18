package org.cqfn.save.backend.utils

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainerProvider
import org.testcontainers.containers.wait.strategy.Wait
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A Junit extension for launching MySql container before all tests.
 */
class MySqlExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext?) {
        if (!isDatabaseCreated.getAndSet(true)) {
            val dbContainer: JdbcDatabaseContainer<*> = MySQLContainerProvider()
                .newInstance("8.0.20")
                .withExposedPorts(MYSQL_PORT)
                .withDatabaseName("save_db_test")
                .waitingFor(Wait.forLogMessage("Container is started (JDBC URL: ", 1))
                .apply {
                    start()
                }

            System.setProperty("spring.datasource.url", dbContainer.jdbcUrl)
            System.setProperty("spring.datasource.username", dbContainer.username)
            System.setProperty("spring.datasource.password", dbContainer.password)
        }
    }

    companion object {
        private const val MYSQL_PORT = 3306
        @Suppress("NonBooleanPropertyPrefixedWithIs")
        private val isDatabaseCreated = AtomicBoolean(false)
    }
}
