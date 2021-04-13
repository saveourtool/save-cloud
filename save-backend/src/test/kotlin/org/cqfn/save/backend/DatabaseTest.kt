package org.cqfn.save.backend

import org.cqfn.save.agent.AgentState
import org.cqfn.save.backend.repository.BaseEntityRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.AgentStatus
import org.cqfn.save.entities.TestExecution
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SaveApplication::class])
@ExtendWith(MySqlExtension::class)
class DatabaseTest {
    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var testExecutionRepository: BaseEntityRepository<TestExecution>

    @Autowired
    private lateinit var agentStatusRepository: BaseEntityRepository<AgentStatus>

    @Test
    fun checkProjectDataInDataBase() {
        val projects = projectRepository.findAll()

        assertTrue(projects.any { it.name == "huaweiName" && it.owner == "Huawei" && it.url == "huaweiUrl" })
    }

    @Test
    fun checkTestStatusDataInDataBase() {
        val tests = testExecutionRepository.findAll()

        assertTrue(tests.any { it.status == TestResultStatus.FAILED && it.agent.id == 1L })
    }

    @Test
    fun checkAgentStatusDataInDataBase() {
        val agents = agentStatusRepository.findAll()

        assertTrue(agents.any { it.state == AgentState.BUSY && it.agent.id == 2L })
    }
}
