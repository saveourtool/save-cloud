package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.Heartbeat
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.orchestrator.utils.AgentStatusInMemoryRepository

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Background inspector, which detect crashed agents
 * TODO: can be used to store data about existing agents on orchestrator startup ([#11](https://github.com/saveourtool/save-cloud/issues/11))
 */
@Component
class HeartBeatInspector(
    private val containerService: ContainerService,
    private val agentService: AgentService,
    private val agentStatusInMemoryRepository: AgentStatusInMemoryRepository,
) {
    /**
     * Collect information about the latest heartbeats from agents, in aim to determine crashed one later
     *
     * @param heartbeat
     */
    fun updateAgentHeartbeatTimeStamps(heartbeat: Heartbeat) {
        agentStatusInMemoryRepository.upsert(
            AgentStatusDto(
                containerId = heartbeat.agentInfo.containerId,
                state = heartbeat.state,
                time = heartbeat.timestamp.toLocalDateTime(TimeZone.UTC)
            ),
            executionId = heartbeat.executionProgress.executionId,
        )
    }

    /**
     * @param containerId
     */
    fun unwatchAgent(containerId: String) {
        agentStatusInMemoryRepository.delete(containerId)
    }

    /**
     * @param containerId
     */
    fun watchCrashedAgent(containerId: String) {
        agentStatusInMemoryRepository.markAsCrashed(containerId)
    }

    /**
     * Consider agent as crashed, if it didn't send heartbeats for some time
     */
    fun determineCrashedAgents() {
        agentStatusInMemoryRepository.updateByStatus { containerId -> containerService.isStoppedByContainerId(containerId) }
    }

    /**
     * Stop crashed agents and mark corresponding test executions as failed with internal error
     */
    fun processExecutionWithCrashedAgents() {
        agentStatusInMemoryRepository.processExecutionWithAllCrashedContainers { executionIds ->
            executionIds.forEach { executionId ->
                logger.warn("All agents for execution $executionId are crashed, initialize finalization of it.")
                agentService.finalizeExecution(executionId)
            }
        }
    }

    @Scheduled(cron = "*/\${orchestrator.heart-beat-inspector-interval} * * * * ?")
    private fun run() {
        determineCrashedAgents()
        processExecutionWithCrashedAgents()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HeartBeatInspector::class.java)
    }
}
