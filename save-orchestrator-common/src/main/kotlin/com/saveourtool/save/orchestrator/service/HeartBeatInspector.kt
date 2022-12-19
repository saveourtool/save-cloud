package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.Heartbeat
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.utils.OrchestratorAgentStatusService
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * Background inspector, which detect crashed agents
 * TODO: can be used to store data about existing agents on orchestrator startup ([#11](https://github.com/saveourtool/save-cloud/issues/11))
 */
@Component
class HeartBeatInspector(
    configProperties: ConfigProperties,
    private val containerService: ContainerService,
    private val agentService: AgentService,
) {
    /**
     * Collection that stores information about containers
     */
    internal val orchestratorAgentStatusService = OrchestratorAgentStatusService(configProperties.agentsHeartBeatTimeoutMillis)

    /**
     * Collect information about the latest heartbeats from agents, in aim to determine crashed one later
     *
     * @param heartbeat
     */
    fun updateAgentHeartbeatTimeStamps(heartbeat: Heartbeat) {
        orchestratorAgentStatusService.upsert(
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
        orchestratorAgentStatusService.delete(containerId)
    }

    /**
     * @param containerId
     */
    fun watchCrashedAgent(containerId: String) {
        orchestratorAgentStatusService.markAsCrashed(containerId)
    }

    /**
     * Consider agent as crashed, if it didn't send heartbeats for some time
     */
    fun determineCrashedAgents() {
        orchestratorAgentStatusService.updateByStatus { containerId -> containerService.isStoppedByContainerId(containerId) }
    }

    /**
     * Stop crashed agents and mark corresponding test executions as failed with internal error
     */
    fun processCrashedAgents() {
        orchestratorAgentStatusService.processCrashed { crashedAgents ->
            logger.debug("Stopping crashed agents: $crashedAgents")

            val areAgentsStopped = containerService.stopAgents(crashedAgents)
            if (areAgentsStopped) {
                Flux.fromIterable(crashedAgents).flatMap { containerId ->
                    agentService.updateAgentStatusesWithDto(AgentStatusDto(AgentState.CRASHED, containerId))
                }.blockLast()
                orchestratorAgentStatusService.processExecutionWithoutNotCrashedContainers { executionIds ->
                    executionIds.forEach { executionId ->
                        logger.warn("All agents for execution $executionId are crashed, initialize cleanup for it.")
                        orchestratorAgentStatusService.deleteAllByExecutionId(executionId)
                        agentService.finalizeExecution(executionId)
                    }
                }
            } else {
                logger.warn("Crashed agents $crashedAgents are not stopped after stop command")
            }
        }
    }

    @Scheduled(cron = "*/\${orchestrator.heart-beat-inspector-interval} * * * * ?")
    private fun run() {
        determineCrashedAgents()
        processCrashedAgents()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HeartBeatInspector::class.java)
    }
}
