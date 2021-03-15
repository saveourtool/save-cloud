package org.cqfn.save.backend

import org.cqfn.save.agent.AgentState
import org.cqfn.save.backend.repository.AgentStatusRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestStatusRepository
import org.cqfn.save.backend.utils.DatabaseTestBase
import org.cqfn.save.test_status.TestResultStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SaveApplication::class])
class DatabaseTest : DatabaseTestBase() {
    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var testStatusRepository: TestStatusRepository

    @Autowired
    private lateinit var agentStatusRepository: AgentStatusRepository

    @Test
    fun checkProjectDataInDataBase() {
        val projects = projectRepository.findAll()

        assertTrue(projects.any { it.name == "huaweiName" && it.owner == "Huawei" && it.url == "huaweiUrl" })
    }

    @Test
    fun checkTestStatusDataInDataBase() {
        val tests = testStatusRepository.findAll()

        assertTrue(tests.any { it.status == TestResultStatus.PASSED && it.agentId == "cool_agent_id" })
    }

    @Test
    fun checkAgentStatusDataInDataBase() {
        val agents = agentStatusRepository.findAll()

        assertTrue(agents.any { it.state == AgentState.BUSY && it.agentId == "cool_agent_id" })
    }
}
