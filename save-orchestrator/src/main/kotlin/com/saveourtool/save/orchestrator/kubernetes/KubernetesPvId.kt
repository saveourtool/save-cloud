package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.service.PersistentVolumeId

/**
 * @property pvcName ID of the Kubernetes PVC
 */
data class KubernetesPvId(
    val pvcName: String
) : PersistentVolumeId
