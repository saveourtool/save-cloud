package com.saveourtool.save.orchestrator.service

import java.nio.file.Path

interface PersistentVolumeService {
    /**
     * @return identifier of the created module
     */
    fun createFromResources(resources: Collection<Path>): PersistentVolumeId
}

interface PersistentVolumeId