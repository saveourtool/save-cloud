package org.cqfn.save.backend

import org.cqfn.save.backend.entities.TestEntity
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestEntityRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest(classes = [SaveApplication::class])
class H2DatabaseTest {
    @Autowired
    private lateinit var testEntityRepository: TestEntityRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Test
    fun checkSaveToDatabase() {
        val testEntity: TestEntity = testEntityRepository.save(TestEntity("test"))
        val foundEntity: TestEntity = testEntityRepository.findById(testEntity.id!!).get()
        assertNotNull(foundEntity)
        assertEquals(testEntity.value, foundEntity.value)
    }

    @Test
    fun checkTestDataInDataBase() {
        val projects = projectRepository.findAll()

        assertTrue(projects.any { it.name == "huaweiName" && it.owner == "Huawei" && it.url == "huawei.com" })
    }
}
