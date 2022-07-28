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
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
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
        if (resources.size > 1) {
            TODO("Not yet implemented")
        }

        @Language("yaml")
        val resource = kc.resource(
            """
                |apiVersion: v1
                |kind: PersistentVolumeClaim
                |metadata:
                |  generateName: save-execution-pv-
                |  namespace: ${configProperties.kubernetes.namespace}
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
                    pvcStorageSpec.lines().joinToString("\n") { "|  $it\n" }
                }}
            """.trimMargin().also {
                logger.debug { "Creating PVC from the following YAML:\n${it.lines().joinToString(System.lineSeparator()) { it.prependIndent(" ".repeat(4)) }}" }
            }
        ) as NamespaceableResource<PersistentVolumeClaim>

        val persistentVolumeClaim = resource.create()

        val sourceVolumeName = UUID.randomUUID()
        @Language("yaml")
        val sourceResourceVolume = kc.resource(
            """
                |apiVersion: v1
                |kind: PersistentVolume
                |metadata:
                |  name: $sourceVolumeName
                |  namespace: ${configProperties.kubernetes.namespace}
                |spec:
                |  accessModes:
                |    - ReadWriteOnce
                |  capacity:
                |    storage: ${configProperties.kubernetes.pvcSize}
                |  hostPath:
                |    path: ${resources.single().absolutePathString()}
            """.trimMargin().also {
                logger.debug { "Creating PV from the following YAML:\n${it.lines().joinToString(System.lineSeparator()) { it.prependIndent(" ".repeat(4)) }}" }
            }
        )
        sourceResourceVolume.create()

        @Language("yaml")
        val sourceResource = kc.resource(
            """
                |apiVersion: v1
                |kind: PersistentVolumeClaim
                |metadata:
                |  generateName: save-execution-source-
                |  namespace: ${configProperties.kubernetes.namespace}
                |spec:
                |  accessModes:
                |    - ReadWriteOnce
                |  storageClassName: ""  # Empty string matches volume created on the previous step
                |  resources:
                |    requests:
                |      storage: ${configProperties.kubernetes.pvcSize}
                |  volumeName: $sourceVolumeName
            """.trimMargin().also {
                logger.debug { "Creating PVC from the following YAML:\n${it.lines().joinToString(System.lineSeparator()) { it.prependIndent(" ".repeat(4)) }}" }
            }
        ) as NamespaceableResource<PersistentVolumeClaim>
        val sourcePvc = sourceResource.create()

        return KubernetesPvId(persistentVolumeClaim.metadata.name, sourcePvc.metadata.name)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesPersistentVolumeService::class.java)
    }
}
