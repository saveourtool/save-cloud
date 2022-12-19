package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.Heartbeat
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.utils.ContainersCollection

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
    internal val containersCollection = ContainersCollection(configProperties.agentsHeartBeatTimeoutMillis)

    /**
     * Collect information about the latest heartbeats from agents, in aim to determine crashed one later
     *
     * @param heartbeat
     */
    fun updateAgentHeartbeatTimeStamps(heartbeat: Heartbeat) {
        containersCollection.upsert(
            containerId = heartbeat.agentInfo.containerId,
            executionId = heartbeat.executionProgress.executionId,
            timestamp = heartbeat.timestamp,
        )
    }

    /**
     * @param containerId
     */
    fun unwatchAgent(containerId: String) {
        containersCollection.delete(containerId)
    }

    /**
     * @param containerId
     */
    fun watchCrashedAgent(containerId: String) {
        containersCollection.markAsCrashed(containerId)
    }

    /**
     * Consider agent as crashed, if it didn't send heartbeats for some time
     */
    fun determineCrashedAgents() {
        containersCollection.updateByStatus { containerId -> containerService.isStoppedByContainerId(containerId) }
    }

    /**
     * Stop crashed agents and mark corresponding test executions as failed with internal error
     */
    fun processCrashedAgents() {
        containersCollection.processCrashed { crashedAgents ->
            logger.debug("Stopping crashed agents: $crashedAgents")

            val areAgentsStopped = containerService.stopAgents(crashedAgents)
            if (areAgentsStopped) {
                Flux.fromIterable(crashedAgents).flatMap { containerId ->
                    agentService.updateAgentStatusesWithDto(AgentStatusDto(AgentState.CRASHED, containerId))
                }.blockLast()
                containersCollection.processExecutionWithoutNotCrashedContainers { executionIds ->
                    executionIds.forEach { executionId ->
                        logger.warn("All agents for execution $executionId are crashed, initialize cleanup for it.")
                        containersCollection.deleteAllByExecutionId(executionId)
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
