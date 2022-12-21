package com.saveourtool.save.orchestrator.service

import com.saveourtool.save.agent.Heartbeat
import com.saveourtool.save.orchestrator.config.ConfigProperties

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

typealias AgentStateWithTimeStamp = Pair<String, Instant>

/**
 * Background inspector, which detect crashed agents
 * TODO: can be used to store data about existing agents on orchestrator startup ([#11](https://github.com/saveourtool/save-cloud/issues/11))
 */
@Component
class HeartBeatInspector(
    private val configProperties: ConfigProperties,
    private val containerService: ContainerService,
    private val agentService: AgentService,
) {
    private val agentsLatestHeartBeatsMap: ConcurrentMap<String, AgentStateWithTimeStamp> = ConcurrentHashMap()

    /**
     * Collection that stores agents that are acting abnormally and will probably be terminated forcefully
     */
    internal val crashedAgents: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Collect information about the latest heartbeats from agents, in aim to determine crashed one later
     *
     * @param heartbeat
     */
    fun updateAgentHeartbeatTimeStamps(heartbeat: Heartbeat) {
        agentsLatestHeartBeatsMap[heartbeat.agentInfo.containerId] = heartbeat.state.name to heartbeat.timestamp
    }

    /**
     * @param containerId
     */
    fun unwatchAgent(containerId: String) {
        agentsLatestHeartBeatsMap.remove(containerId)
        crashedAgents.remove(containerId)
    }

    /**
     * @param containerId
     */
    fun watchCrashedAgent(containerId: String) {
        crashedAgents.add(containerId)
    }

    /**
     * @param containerId
     */
    fun unwatchCrashedAgent(containerId: String) {
        crashedAgents.remove(containerId)
    }

    /**
     * Consider agent as crashed, if it didn't send heartbeats for some time
     */
    fun determineCrashedAgents() {
        agentsLatestHeartBeatsMap.filter { (currentContainerId, _) ->
            currentContainerId !in crashedAgents
        }.forEach { (currentContainerId, stateToLatestHeartBeatPair) ->
            val duration = (Clock.System.now() - stateToLatestHeartBeatPair.second).inWholeMilliseconds
            logger.debug("Latest heartbeat from $currentContainerId was sent: $duration ms ago")
            if (duration >= configProperties.agentsHeartBeatTimeoutMillis) {
                logger.debug("Adding $currentContainerId to list crashed agents")
                crashedAgents.add(currentContainerId)
            }
        }

        crashedAgents.removeIf { containerId ->
            containerService.isStopped(containerId)
        }
        agentsLatestHeartBeatsMap.filterKeys { containerId ->
            containerService.isStopped(containerId)
        }.forEach { (containerId, _) ->
            logger.debug("Agent $containerId is already stopped, will stop watching it")
            agentsLatestHeartBeatsMap.remove(containerId)
        }
    }

    /**
     * Stop crashed agents and mark corresponding test executions as failed with internal error
     */
    fun processCrashedAgents() {
        if (crashedAgents.isEmpty()) {
            return
        }
        logger.debug("Detected crashed agents: $crashedAgents")
        if (agentsLatestHeartBeatsMap.keys.toList() == crashedAgents.toList()) {
            logger.warn("All agents are crashed, initialize shutdown sequence. Crashed agents: $crashedAgents")
            // fixme: should be cleared only for execution
            val containerId = crashedAgents.first()
            agentsLatestHeartBeatsMap.clear()
            crashedAgents.clear()
            agentService.finalizeExecution(containerId)
        }
    }

    @Scheduled(cron = "*/\${orchestrator.heart-beat-inspector-interval} * * * * ?")
    private fun run() {
        determineCrashedAgents()
        processCrashedAgents()
    }

    /**
     * Clear all data about agents
     */
    internal fun clear() {
        agentsLatestHeartBeatsMap.clear()
        crashedAgents.clear()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HeartBeatInspector::class.java)
    }
}
