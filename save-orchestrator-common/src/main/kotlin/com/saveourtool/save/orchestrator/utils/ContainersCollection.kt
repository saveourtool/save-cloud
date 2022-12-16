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
    private val crashedThreshold: Long,
) {
    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    private val executionToContainers: MutableMap<Long, Set<String>> = HashMap()
    private val containerToLatestState: MutableMap<String, Instant> = HashMap()
    private val crashedContainers: MutableSet<String> = HashSet()

    fun upsert(
        containerId: String,
        executionId: Long,
        timestamp: Instant,
    ) {
        useWriteLock {
            executionToContainers[executionId] = executionToContainers[executionId].orEmpty() + containerId
            containerToLatestState[containerId] = timestamp
        }
    }

    fun markAsCrashed(
        containerId: String,
    ) {
        useWriteLock {
            require(executionToContainers.any { (_, containerIds) -> containerIds.contains(containerId) }) {
                "Invalid containerId $containerId: it's not assigned to any execution"
            }
            crashedContainers.add(containerId)
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

    fun deleteAll(
        containerIds: Set<String>
    ) {
        useWriteLock {
            executionToContainers.keys.forEach { executionId ->
                executionToContainers.computeIfPresent(executionId) { _, currentContainerIds ->
                    currentContainerIds - containerIds
                }
            }
            containerToLatestState.keys.removeAll(containerIds)
            crashedContainers.removeAll(containerIds)
        }
    }

    fun containsAnyByExecutionId(
        executionId: Long,
    ) = useReadLock {
        !executionToContainers[executionId].isNullOrEmpty()
    }

    fun processCrashed(process: (Set<String>) -> Unit) {
        useReadLock {
            if (crashedContainers.isNotEmpty()) {
                process(crashedContainers.toSet())
            }
        }
    }

    fun getExecutionWithoutContainers(): Set<Long> = useReadLock {
        executionToContainers.filterValues { it.isEmpty() }.keys
    }

    fun updateByStatus(isStoppedFunction: (String) -> Boolean) {
        useWriteLock {
            containerToLatestState.filter { (currentContainerId, _) ->
                currentContainerId !in crashedContainers
            }.forEach { (currentContainerId, timestamp) ->
                val duration = (Clock.System.now() - timestamp).inWholeMilliseconds
                log.debug {
                    "Latest heartbeat from $currentContainerId was sent: $duration ms ago"
                }
                if (duration >= crashedThreshold) {
                    log.debug("Adding $currentContainerId to list crashed agents")
                    crashedContainers.add(currentContainerId)
                }
            }
            executionToContainers.forEach { (executionId, containerIds) ->
                val stoppedContainersIds = containerIds.filterTo(HashSet(), isStoppedFunction)
                executionToContainers[executionId] = containerIds - stoppedContainersIds
                containerToLatestState.keys.removeAll(containerIds)
                crashedContainers.removeAll(containerIds)
            }
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