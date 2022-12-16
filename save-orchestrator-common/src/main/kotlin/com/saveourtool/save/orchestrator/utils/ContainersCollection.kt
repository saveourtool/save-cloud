package com.saveourtool.save.orchestrator.utils

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.orchestrator.service.AgentStateWithTimeStamp
import com.saveourtool.save.utils.debug
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Collection that stores information about containers:
 * 1. Execution ID for which container is assigned to
 * 2. Latest timestamp and state
 * 3. Crashed containers
 *
 * Collection is thread safe
 */
class ContainersCollection(
    private val agentsHeartBeatTimeoutMillis: Long,
) {
    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    private val executionToContainers: MutableMap<Long, Set<String>> = HashMap()
    private val containerToLatestState: MutableMap<String, AgentStateWithTimeStamp> = HashMap()
    private val crashedContainers: MutableSet<String> = HashSet()

    fun upsert(
        containerId: String,
        executionId: Long,
        timestamp: Instant,
        state: AgentState,
    ) {
        useWriteLock {
            executionToContainers[executionId] = executionToContainers[executionId].orEmpty() + containerId
            containerToLatestState[containerId] = state.toString() to timestamp
            if (state in ILLEGAL_AGENT_STATES) {
                log.warn("Agent with containerId=$containerId sent $state status, but should be offline in that case!")
                crashedContainers.add(containerId)
            }
        }
    }

    fun deleteAllByExecutionId(
        executionId: Long,
    ) {
        useWriteLock {
            executionToContainers.remove(executionId)
                ?.let {
                    containerToLatestState.keys.removeAll(it)
                    crashedContainers.removeAll(it)
                }
        }
    }

    fun containsAnyByExecutionId(
        executionId: Long,
    ) = useReadLock {
        !executionToContainers[executionId].isNullOrEmpty()
    }

    fun recalculateCrashed() {
        useWriteLock {
            containerToLatestState.filter { (currentContainerId, _) ->
                currentContainerId !in crashedContainers
            }.forEach { (currentContainerId, stateToLatestHeartBeatPair) ->
                val duration = (Clock.System.now() - stateToLatestHeartBeatPair.second).inWholeMilliseconds
                log.debug {
                    "Latest heartbeat from $currentContainerId was sent: $duration ms ago"
                }
                if (duration >= agentsHeartBeatTimeoutMillis) {
                    log.debug("Adding $currentContainerId to list crashed agents")
                    crashedContainers.add(currentContainerId)
                }
            }
        }
    }

    fun processCrashed(process: (Set<String>) -> Unit) {
        useReadLock {
            process(crashedContainers.toSet())
        }
    }

    private fun <R> useReadLock(action: () -> R): R = lock.readLock().use(action)
    private fun <R> useWriteLock(action: () -> R): R = lock.writeLock().use(action)

    companion object {
        private val log = LoggerFactory.getLogger(ContainersCollection::class.java)
        private val ILLEGAL_AGENT_STATES = setOf(AgentState.CRASHED, AgentState.TERMINATED, AgentState.STOPPED_BY_ORCH)

        private fun <R> Lock.use(action: () -> R): R {
            lock()
            try {
                return action()
            } finally {
                unlock()
            }
        }
    }
}