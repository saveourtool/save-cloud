package org.cqfn.save.backend

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.utils.MySqlExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

// @WebFluxTest

@DataJpaTest
@EnableJpaRepositories(basePackageClasses = [ProjectRepository::class])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MySqlExtension::class, SpringExtension::class)
@EnableConfigurationProperties(ConfigProperties::class)
@TestPropertySource("classpath:application.properties")
@Import(
    ProjectRepository::class,
    UserRepository::class,
    ProjectService::class,
)
class ProjectServiceTest {
    @Autowired
    private lateinit var projectService: ProjectService

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun `should update an existing project`() {
        val existingProject = projectRepository.findAll().first()
        projectService.saveProject(
            existingProject.apply {
                this.description = "Updated description"
            }
        )

        val updatedProject = projectRepository.findAll().first()
        Assertions.assertEquals("Updated description", updatedProject.description)
        Assertions.assertEquals(1, updatedProject.userId)
    }
}
