/**
 * Interact with persistent volumes of a certain storage system.
 */

package com.saveourtool.save.orchestrator.service

import java.nio.file.Path

/**
 * Interface that lists method to interact with persistent volumes of a certain storage system.
 */
interface PersistentVolumeService {
    /**
     * @param resourcesDir a [Path] that should be copied into a persistent volume
     * @return identifier of the created module
     */
    fun createFromResources(resourcesDir: Path): PersistentVolumeId
}

/**
 * Identifier of a persistent volume
 */
interface PersistentVolumeId
