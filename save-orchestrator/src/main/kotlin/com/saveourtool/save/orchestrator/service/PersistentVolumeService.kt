package com.saveourtool.save.orchestrator.service

import java.nio.file.Path

interface PersistentVolumeService {
    /**
     * @param resources list of Paths that should be copied into a persistent volume
     * @return identifier of the created module
     */
    fun createFromResources(resources: Collection<Path>): PersistentVolumeId
}

interface PersistentVolumeId
