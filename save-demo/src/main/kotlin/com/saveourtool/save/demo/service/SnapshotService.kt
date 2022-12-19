package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.Snapshot
import com.saveourtool.save.demo.repository.SnapshotRepository
import org.springframework.stereotype.Service

@Service
class SnapshotService(
    private val snapshotRepository: SnapshotRepository,
) {
    /**
     * @param snapshot
     */
    fun save(snapshot: Snapshot): Snapshot = snapshotRepository.save(snapshot)

    /**
     * @param snapshot
     * @return
     */
    fun saveIfNotPresent(snapshot: Snapshot): Snapshot = snapshotRepository.findByVersionAndExecutableName(
        snapshot.version,
        snapshot.executableName,
    ) ?: save(snapshot)
}
