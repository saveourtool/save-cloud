package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.service.PersistentVolumeId

/**
 * @property pvcName name of the Kubernetes PVC
 * @property sourcePvcName name of the PVC which contains original resources that should be copied into [pvcName]
 */
data class KubernetesPvId(
    val pvcName: String,
    val sourcePvcName: String,
    val sourcePath: String,
) : PersistentVolumeId
