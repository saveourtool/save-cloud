package com.saveourtool.save.orchestrator.docker

import com.saveourtool.save.orchestrator.service.PersistentVolumeId

/**
 * @property volumeName name of the Docker volume
 */
data class DockerPvId(
    val volumeName: String,
) : PersistentVolumeId
