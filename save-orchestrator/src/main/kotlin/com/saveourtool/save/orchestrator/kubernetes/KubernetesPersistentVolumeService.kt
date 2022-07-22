package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.service.PersistentVolumeId
import com.saveourtool.save.orchestrator.service.PersistentVolumeService
import io.fabric8.kubernetes.api.model.HostPathVolumeSource
import io.fabric8.kubernetes.api.model.PersistentVolume
import io.fabric8.kubernetes.api.model.PersistentVolumeSpec
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createTempDirectory

@Profile("kubernetes")
@Component
class KubernetesPersistentVolumeService(
    private val kc: KubernetesClient,
) : PersistentVolumeService {
    override fun createFromResources(resources: Collection<Path>): KubernetesPvId {
        val tmpDir = createTempDirectory()
        resources.forEach {
            it.copyTo(tmpDir)
        }

        val persistentVolume = kc.persistentVolumes().create(
            PersistentVolume().apply {
                spec = PersistentVolumeSpec().apply {
                    hostPath = HostPathVolumeSource().apply {
                        path = tmpDir.absolutePathString()
                    }
                }
            }
        )
        return KubernetesPvId(persistentVolume.fullResourceName)
    }
}

data class KubernetesPvId(
    val id: String
) : PersistentVolumeId
