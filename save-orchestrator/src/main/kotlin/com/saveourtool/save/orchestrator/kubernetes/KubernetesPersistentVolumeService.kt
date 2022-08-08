package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.runner.SAVE_AGENT_USER_HOME
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
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

/**
 * Implementation of [PersistentVolumeService] that creates Persistent Volumes in Kubernetes
 */
@Profile("kubernetes")
@Component
class KubernetesPersistentVolumeService(
    private val kc: KubernetesClient,
    private val configProperties: ConfigProperties,
) : PersistentVolumeService {
    @Suppress("TOO_LONG_FUNCTION")
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
                        pvcAnnotations.lines().joinToString("\n") { "|    $it\n" }
            }}
                |  
                |spec:
                |  accessModes:
                |    - ReadWriteMany
                |  resources:
                |    requests:
                |      storage: ${configProperties.kubernetes.pvcSize}
                |#  NB: key `volumeName` is not needed here, otherwise provisioner won't attempt to create a PV automatically
                ${configProperties.kubernetes.pvcStorageSpec.let { pvcStorageSpec ->
                pvcStorageSpec.lines().joinToString("\n") { "|  $it\n" }
            }}
            """.trimMargin().also {
                logger.debug { "Creating PVC from the following YAML:\n${it.asIndentedMultiline()}" }
            }
        ) as NamespaceableResource<PersistentVolumeClaim>

        val persistentVolumeClaim = resource.create()

        val resourcesRelativePath = resources.single().relativeTo(
            Paths.get(configProperties.testResources.tmpPath)
        )
        val intermediateResourcesPath = "$SAVE_AGENT_USER_HOME/tmp"
        return KubernetesPvId(
            persistentVolumeClaim.metadata.name,
            "tmp-resources-storage",
            "$intermediateResourcesPath/${resourcesRelativePath.pathString}"
        )
    }

    @Suppress("MAGIC_NUMBER")
    private fun String.asIndentedMultiline(indent: Int = 4) = lines()
        .joinToString(System.lineSeparator()) {
            it.prependIndent(" ".repeat(indent))
        }

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesPersistentVolumeService::class.java)
    }
}
