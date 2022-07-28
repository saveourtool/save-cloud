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
     * @param resources list of Paths that should be copied into a persistent volume
     * @return identifier of the created module
     */
    fun createFromResources(resources: Collection<Path>): PersistentVolumeId

    /**
     * Delete a persistent volume denoted by id [id].
     */
//    fun delete(id: PersistentVolumeId)
}

/**
 * Identifier of a persistent volume
 */
interface PersistentVolumeId
