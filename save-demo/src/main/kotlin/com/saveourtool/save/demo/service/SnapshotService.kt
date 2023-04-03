package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.Snapshot
import com.saveourtool.save.demo.repository.SnapshotRepository
import org.springframework.stereotype.Service

/**
 * [Service] for [Snapshot] entity
 */
@Service
class SnapshotService(
    private val snapshotRepository: SnapshotRepository,
) {
    private fun save(snapshot: Snapshot): Snapshot = snapshotRepository.save(snapshot)

    /**
     * @param snapshot
     * @return [Snapshot] entity saved to data
     */
    fun saveIfNotPresent(snapshot: Snapshot): Snapshot = snapshotRepository.findByVersionAndExecutableName(
        snapshot.version,
        snapshot.executableName,
    ) ?: save(snapshot)
}
