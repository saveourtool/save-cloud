package org.cqfn.save.orchestrator.database

import org.cqfn.save.agent.AgentState
import org.cqfn.save.orchestrator.SaveOrchestrator
import org.cqfn.save.orchestrator.repository.AgentStatusRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [SaveOrchestrator::class])
@ActiveProfiles("dev")
class AgentStatusTest {
    @Autowired
    private lateinit var agentStatusRepository: AgentStatusRepository

    @Test
    fun `check that agent status is in the database`() {
        val statuses = agentStatusRepository.findAll()
        assertTrue(statuses.any { it.state == AgentState.IDLE && it.time.toString() == "2021-12-31" })
    }
}