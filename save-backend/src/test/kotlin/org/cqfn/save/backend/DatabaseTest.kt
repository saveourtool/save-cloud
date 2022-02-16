package org.cqfn.save.backend

import org.cqfn.save.agent.AgentState
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.domain.TestResultStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MySqlExtension::class)
class DatabaseTest {
    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    @Autowired
    private lateinit var agentStatusRepository: AgentStatusRepository

    @Test
    fun checkProjectDataInDataBase() {
        val projects = projectRepository.findAll()

        assertTrue(projects.any { it.name == "huaweiName" && it.organization.name == "Huawei" && it.url == "huawei.com" })
    }

    @Test
    fun checkTestStatusDataInDataBase() {
        val tests = testExecutionRepository.findAll()

        assertTrue(tests.any { it.status == TestResultStatus.FAILED && it.agent?.id == 1L })
    }

    @Test
    fun checkAgentStatusDataInDataBase() {
        val agents = agentStatusRepository.findAll()

        assertTrue(agents.any { it.state == AgentState.BUSY && it.agent.id == 2L })
    }
}
