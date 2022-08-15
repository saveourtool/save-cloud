package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.service.PersistentVolumeId

/**
 * @property pvcName name of the Kubernetes PVC
 * @property sourcePvcName name of the PVC which contains original resources that should be copied into [pvcName]
 * Fixme: now it should be passed from PersistentVolumeManager into Runner because it will mount it to the init container.
 * @property sourcePath path to the stored resources on the intermediate PV
 */
data class KubernetesPvId(
    val pvcName: String,
    val sourcePvcName: String,
    val sourcePath: String,
) : PersistentVolumeId
