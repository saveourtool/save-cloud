package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.orchestrator.service.PersistentVolumeId
import com.saveourtool.save.orchestrator.service.PersistentVolumeService
import java.nio.file.Path

class DockerPersistentVolumeService : PersistentVolumeService {
    override fun createFromResources(resources: Collection<Path>): DockerPvId {
        TODO("Not yet implemented")
    }
}

data class DockerPvId(
    val hostPath: String,
) : PersistentVolumeId
