package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.service.PersistentVolumeService
import com.saveourtool.save.utils.debug
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.NamespaceableResource
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.copyTo
import kotlin.io.path.createTempDirectory

/**
 * Implementation of [PersistentVolumeService] that creates Persistent Volumes in Kubernetes
 */
@Profile("kubernetes")
@Component
class KubernetesPersistentVolumeService(
    private val kc: KubernetesClient,
    private val configProperties: ConfigProperties,
) : PersistentVolumeService {
    override fun createFromResources(resources: Collection<Path>): KubernetesPvId {
        requireNotNull(configProperties.kubernetes)

        val tmpDir = createTempDirectory()
        resources.forEach {
            it.copyTo(tmpDir.resolve(it))
        }

        @Language("yaml")
        val resource = kc.resource(
            """
                |apiVersion: v1
                |kind: PersistentVolumeClaim
                |metadata:
                |  generateName: save-execution-pv
                |${configProperties.kubernetes.pvcAnnotations?.let { pvcAnnotations ->
                "  annotations:\n" +
                    pvcAnnotations.lines().joinToString { "|    $it\n" }
                }}
                |  
                |spec:
                |  accessModes:
                |    - ReadWriteMany
                |  resources:
                |    requests:
                |      storage: ${configProperties.kubernetes.pvcSize}
                |#  NB: key `volumeName` is not needed here, otherwise provisioner won't attempt to create a PV automatically
                |#  storageClassName: ${""/*configProperties.kubernetes.pvcStorageClass*/}
                ${configProperties.kubernetes.pvcStorageSpec.let { pvcStorageSpec ->
                    pvcStorageSpec.lines().joinToString { "|  $it\n" }
                }}
            """.trimMargin().also {
                logger.debug { "Creating PVC from the following YAML:\n${it.lines().joinToString(System.lineSeparator()) { it.prependIndent(" ".repeat(4)) }}" }
            }
        ) as NamespaceableResource<PersistentVolumeClaim>

        val persistentVolumeClaim = resource.create()
        return KubernetesPvId(persistentVolumeClaim.metadata.name)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesPersistentVolumeService::class.java)
    }
}
