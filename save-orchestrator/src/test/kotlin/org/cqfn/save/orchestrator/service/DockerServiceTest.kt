package org.cqfn.save.orchestrator.service

import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

@ExtendWith(SpringExtension::class)
//@ContextConfiguration(classes = [ConfigProperties::class, DockerService::class])
@EnableConfigurationProperties(ConfigProperties::class)
//@ConfigurationPropertiesScan("org.cqfn.save.orchestrator.config")
@TestPropertySource("classpath:application.properties")
class DockerServiceTest {
    @Autowired private lateinit var configProperties: ConfigProperties

    private lateinit var dockerService: DockerService

    @BeforeEach
    fun setUp() {
        dockerService = DockerService(configProperties)
    }

    @Test
    fun `test`() {
        val testExecution = Execution(0, LocalDateTime.now(), LocalDateTime.now(), ExecutionStatus.PENDING, "1", "foo")
        dockerService.buildAndCreateContainer(testExecution)
    }

    companion object {
        @OptIn(ExperimentalPathApi::class)
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("orchestrator.testResources.basePath") {
                val tmpDir = createTempDirectory("repository")
                Path(tmpDir.pathString, "foo").createDirectory()
                tmpDir.pathString
            }
        }
    }
}
