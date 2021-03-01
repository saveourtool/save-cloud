package org.cqfn.save.backend

import org.cqfn.save.backend.repository.ProjectRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainerProvider
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(classes = [SaveApplication::class])
@ActiveProfiles("dev")
@Testcontainers
class DatabaseTest {
    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun checkTestDataInDataBase() {
        val projects = projectRepository.findAll()

        assertTrue(projects.any { it.name == "huaweiName" && it.owner == "Huawei" && it.url == "huawei.com" })
    }

    companion object {
        @Container
        @JvmStatic
        val dbContainer: JdbcDatabaseContainer<*> = MySQLContainerProvider()
            .newInstance("8.0")
            .withExposedPorts(3306)
            .withDatabaseName("db_example")
            .waitingFor(Wait.forLogMessage("Container is started (JDBC URL: ", 1))

        @DynamicPropertySource
        @JvmStatic
        fun dbProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { dbContainer.jdbcUrl }
            registry.add("spring.datasource.username") { dbContainer.username }
            registry.add("spring.datasource.password") { dbContainer.password }
        }
    }
}
