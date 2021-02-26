package org.cqfn.save.backend

import org.cqfn.save.backend.repository.ProjectRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [SaveApplication::class])
@ActiveProfiles("dev")
class H2DatabaseTest {
    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun checkTestDataInDataBase() {
        val projects = projectRepository.findAll()

        assertTrue(projects.any { it.name == "huaweiName" && it.owner == "Huawei" && it.url == "huawei.com" })
    }
}
