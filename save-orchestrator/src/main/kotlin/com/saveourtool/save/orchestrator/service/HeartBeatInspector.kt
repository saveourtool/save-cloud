package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.Heartbeat
import com.saveourtool.save.entities.AgentStatusDto
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.controller.HeartbeatController
import com.saveourtool.save.orchestrator.controller.agentsLatestHeartBeatsMap
import com.saveourtool.save.orchestrator.controller.crashedAgentsList
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.PropertySource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Background inspector, which detect crashed agents
 * TODO: can be used to store data about existing agents on orchestrator startup ([#11](https://github.com/saveourtool/save-cloud/issues/11))
 */
@Component
@PropertySource("classpath:application.properties")
class HeartBeatInspector(
    private val configProperties: ConfigProperties,
    private val dockerService: DockerService,
    private val agentService: AgentService,
) {
    /**
     * Collect information about the latest heartbeats from agents, in aim to determine crashed one later
     *
     * @param heartbeat
     */
    fun updateAgentHeartbeatTimeStamps(heartbeat: Heartbeat) {
        agentsLatestHeartBeatsMap[heartbeat.agentId] = heartbeat.state.name to heartbeat.timestamp
    }

    /**
     * Consider agent as crashed, if it didn't send heartbeats for some time
     */
    fun determineCrashedAgents() {
        agentsLatestHeartBeatsMap.filter { (currentAgentId, _) ->
            currentAgentId !in crashedAgentsList
        }.forEach { (currentAgentId, stateToLatestHeartBeatPair) ->
            val duration = (Clock.System.now() - stateToLatestHeartBeatPair.second).inWholeMilliseconds
            logger.debug("Latest heartbeat from $currentAgentId was sent: $duration ms ago")
            if (duration >= configProperties.agentsHeartBeatTimeoutMillis) {
                logger.debug("Adding $currentAgentId to list crashed agents")
                crashedAgentsList.add(currentAgentId)
            }
        }
    }

    /**
     * Stop crashed agents and mark corresponding test executions as failed with internal error
     */
    fun processCrashedAgents() {
        if (crashedAgentsList.isEmpty()) {
            return
        }
        logger.debug("Stopping crashed agents: $crashedAgentsList")

        val areAgentsStopped = dockerService.stopAgents(crashedAgentsList)
        if (areAgentsStopped) {
            agentService.updateAgentStatusesWithDto(
                crashedAgentsList.map { agentId ->
                    AgentStatusDto(LocalDateTime.now(), AgentState.CRASHED, agentId)
                }
            )
            if (agentsLatestHeartBeatsMap.keys().toList() == crashedAgentsList.toList()) {
                logger.warn("All agents are crashed, initialize shutdown sequence")
                // fixme: should be cleared only for execution
                val agentId = crashedAgentsList.first()
                agentsLatestHeartBeatsMap.clear()
                crashedAgentsList.clear()
                agentService.initiateShutdownSequence(agentId, areAllAgentsCrashed = true)
            }
        } else {
            logger.warn("Crashed agents $crashedAgentsList are not stopped after stop command")
        }
    }

    @Scheduled(cron = "*/\${orchestrator.heartBeatInspectorInterval} * * * * ?")
    private fun run() {
        determineCrashedAgents()
        processCrashedAgents()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HeartBeatInspector::class.java)
    }
}
