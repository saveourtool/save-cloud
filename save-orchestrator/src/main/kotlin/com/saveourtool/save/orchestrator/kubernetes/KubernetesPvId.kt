package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.service.PersistentVolumeId

/**
 * @property id ID of the Kubernetes PV
 */
data class KubernetesPvId(
    val id: String
) : PersistentVolumeId
