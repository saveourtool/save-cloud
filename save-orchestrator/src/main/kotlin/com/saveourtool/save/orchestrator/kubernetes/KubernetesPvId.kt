package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.service.PersistentVolumeId

/**
 * @property pvcId ID of the Kubernetes PVC
 */
data class KubernetesPvId(
    val pvcId: String
) : PersistentVolumeId
