package com.saveourtool.save.orchestrator.utils

import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getCurrentLocalDateTime

import org.slf4j.LoggerFactory

import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Collection that stores information about containers:
 * 1. Execution ID for which container is assigned to
 * 2. Latest timestamp and state
 * 3. Crashed containers
 *
 * Collection is thread safe
 *
 * @property crashedThresholdInMillis threshold in millis to detect crashed containers
 */
class ContainersCollection(
    private val crashedThresholdInMillis: Long,
) {
    private val lock: ReadWriteLock = ReentrantReadWriteLock()

    @Suppress("TYPE_ALIAS")
    private val executionToContainers: MutableMap<Long, Set<String>> = HashMap()
    private val containerToLatestState: MutableMap<String, Instant> = HashMap()
    private val crashedContainers: MutableSet<String> = HashSet()
    private val executionStartTime: MutableMap<Long, LocalDateTime> = HashMap()

    fun markExecutionAsStarted(executionId: Long): Unit = useWriteLock {
        executionStartTime[executionId] = getCurrentLocalDateTime()
    }

    /**
     * Adds or updates information about container.
     * It checks that new information about container contains the original execution id.
     *
     * @param containerId ID of the container
     * @param executionId ID of an execution to which this container is assigned to
     * @param timestamp when **orchestrator** received the latest heartbeat from this container
     */
    fun upsert(
        containerId: String,
        executionId: Long,
        timestamp: Instant,
    ): Unit = useWriteLock {
        val anotherExecutionIds = executionToContainers
            .filterValues { it.contains(containerId) }
            .filterKeys { it != executionId }
            .keys
        require(anotherExecutionIds.isEmpty()) {
            "Invalid containerId $containerId: it's already assigned to another execution $anotherExecutionIds"
        }
        executionToContainers[executionId] = executionToContainers[executionId].orEmpty() + containerId
        containerToLatestState[containerId] = timestamp
    }

    /**
     * Mark container as crashed.
     * It checks that collection knows about provided container.
     *
     * @param containerId ID of the container
     */
    fun markAsCrashed(
        containerId: String,
    ): Unit = useWriteLock {
        require(executionToContainers.any { (_, containerIds) -> containerIds.contains(containerId) }) {
            "Invalid containerId $containerId: it's not assigned to any execution"
        }
        crashedContainers.add(containerId)
    }

    /**
     * Delete all containers assigned to provided execution id and execution id itself.
     * It checks that collection knows about provided execution.
     *
     * @param executionId
     */
    fun deleteAllByExecutionId(
        executionId: Long,
    ): Unit = useWriteLock {
        val assignedContainerIds = requireNotNull(executionToContainers.remove(executionId)) {
            "Invalid executionId $executionId: no container which is assigned to provided execution"
        }
        containerToLatestState.keys.removeAll(assignedContainerIds)
        crashedContainers.removeAll(assignedContainerIds)
        executionStartTime.remove(executionId)
    }

    /**
     * Delete provided container from collections.
     * It doesn't remove execution if all containers are assigned to it are removed.
     *
     * @param containerId container's ID
     */
    fun delete(
        containerId: String,
    ): Unit = deleteAll(setOf(containerId))

    /**
     * Delete provided containers from collections.
     * It doesn't remove execution if all containers are assigned to it are removed.
     *
     * @param containerIds list of container's ID
     */
    fun deleteAll(
        containerIds: Set<String>
    ): Unit = useWriteLock {
        executionToContainers.keys.forEach { executionId ->
            executionToContainers.computeIfPresent(executionId) { _, currentContainerIds ->
                currentContainerIds - containerIds
            }
        }
        containerToLatestState.keys.removeAll(containerIds)
        crashedContainers.removeAll(containerIds)
    }

    /**
     * Check that collection contains any container which is assigned to provided execution.
     *
     * @param executionId ID of the execution
     * @return true if collection contains any container assigned to execution, otherwise -- false
     */
    fun containsAnyByExecutionId(
        executionId: Long,
    ) = useReadLock {
        !executionToContainers[executionId].isNullOrEmpty()
    }

    /**
     * Process crashed containers from this collection.
     * Logic is called only if there are crashed containers.
     *
     * @param process action on list of ID's container which is crashed
     */
    fun processCrashed(process: (Set<String>) -> Unit): Unit = useReadLock {
        if (crashedContainers.isNotEmpty()) {
            process(Collections.unmodifiableSet(crashedContainers))
        }
    }

    /**
     * Process executions without containers are assigned to them.
     * Logic is called only if there are executions without containers.
     *
     * @param process action on execution ids
     */
    fun processExecutionWithoutContainers(process: (Set<Long>) -> Unit): Unit = useReadLock {
        executionToContainers
            .mapNotNullTo(HashSet()) { (key, values) ->
                key.takeIf {
                    (values.isEmpty() || crashedContainers.containsAll(values)) &&
                            requireNotNull(executionStartTime[key]) {
                                "There is no startTime for executionId $key"
                            }
                                .toInstant(TimeZone.UTC)
                                .isStale()
                }
            }
            .takeIf { it.isNotEmpty() }
            ?.let(process)
    }

    /**
     * Update collection by checking status of container.
     * It marks stale containers as crashed.
     *
     * @param isStoppedFunction function which checks that container is stopped.
     */
    fun updateByStatus(isStoppedFunction: (String) -> Boolean): Unit = useWriteLock {
        containerToLatestState.filter { (currentContainerId, _) ->
            currentContainerId !in crashedContainers
        }.forEach { (currentContainerId, timestamp) ->
            log.debug {
                "Latest heartbeat from $currentContainerId was sent: $timestamp"
            }
            if (timestamp.isStale()) {
                log.debug { "Adding $currentContainerId to list crashed agents" }
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

    private fun Instant.isStale(): Boolean = (Clock.System.now() - this).inWholeMilliseconds >= crashedThresholdInMillis

    private fun <R> useReadLock(action: () -> R): R = lock.readLock().use(action)
    private fun <R> useWriteLock(action: () -> R): R = lock.writeLock().use(action)

    companion object {
        private val log = LoggerFactory.getLogger(ContainersCollection::class.java)

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
