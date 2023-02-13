package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.utils.KubernetesRunnerException
import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.utils.debug
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KubernetesService(
    private val kc: KubernetesClient,
    private val configProperties: ConfigProperties,
) {
    private val kubernetesSettings = requireNotNull(configProperties.kubernetes) {
        "demo.kubernetes.* properties are required in this profile"
    }

    private fun jobNameForDemo(demo: Demo) = with(demo) { "demo-${organizationName.lowercase()}-${projectName.lowercase()}-1" }

    /**
     * @param demo
     * @throws KubernetesRunnerException
     */
    @Suppress("NestedBlockDepth")
    fun start(demo: Demo) {
        val job = Job().apply {
            metadata = ObjectMeta().apply {
                name = jobNameForDemo(demo)
            }
            spec = JobSpec().apply {
                parallelism = REPLICAS_PER_DEMO
                ttlSecondsAfterFinished = TTL_AFTER_COMPLETED
                backoffLimit = 0
                template = PodTemplateSpec().apply {
                    spec = PodSpec().apply {
                        if (kubernetesSettings.useGvisor) {
                            nodeSelector = mapOf(
                                "gvisor" to "enabled"
                            )
                            runtimeClassName = "gvisor"
                        }
                        metadata = ObjectMeta().apply {
                            labels = mapOf(
                                DEMO_ORG_NAME to demo.organizationName,
                                DEMO_PROJ_NAME to demo.projectName,
                                DEMO_VERSION to "manual",
                                // "baseImageName" to baseImageName
                                "io.kompose.service" to "save-demo-agent",
                            )
                        }
                        // If agent fails, we should handle it manually (update statuses, attempt restart etc.)
                        restartPolicy = "Never"
                        containers = listOf(agentContainerSpec(demo.sdk.toSdk().baseImageName()))
                    }
                }
            }
        }
        logger.debug { "Attempt to create Job from the following spec: $job" }
        try {
            kc.resource(job)
                .create()
            with(demo) {
                logger.info("Created Job for demo $organizationName/$projectName")
            }
        } catch (kex: KubernetesClientException) {
            with(demo) {
                throw KubernetesRunnerException("Unable to create a job for demo $organizationName/$projectName", kex)
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION")
    private fun agentContainerSpec(imageName: String) = Container().apply {
        name = "save-demo-agent-pod"
        image = imageName
        imagePullPolicy = "IfNotPresent"

        resources = with(kubernetesSettings) {
            ResourceRequirements().apply {
                requests = mapOf(
                    "cpu" to Quantity(agentCpuRequests),
                    "memory" to Quantity(agentMemoryRequests),
                )
                limits = mapOf(
                    "cpu" to Quantity(agentCpuLimits),
                    "memory" to Quantity(agentMemoryLimits),
                )
            }
        }
    }

    /**
     * @param demo
     * @return
     */
    fun stop(demo: Demo): List<StatusDetails> = getPod(demo).delete()

    fun isJobReady(demo: Demo) = kc.batch().v1()
        .jobs()
        .inNamespace(configProperties.kubernetes.namespace)
        .withName(jobNameForDemo(demo))
        .isReady



    private fun getPodByName(podName: String) = kc.pods().withName(podName)

    private fun getPod(demo: Demo) = getPodByName(jobNameForDemo(demo))

    /**
     * @param demo
     * @return
     */
    fun restart(demo: Demo) = stop(demo).also { start(demo) }

    /**
     * @param demo
     * @return
     */
    fun getPodUrl(demo: Demo): String = getPod(demo).get().status.podIP

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesService::class.java)
        private const val DEMO_ORG_NAME = "organizationName"
        private const val DEMO_PROJ_NAME = "projectName"
        private const val DEMO_VERSION = "version"
        private const val REPLICAS_PER_DEMO = 1
        private const val TTL_AFTER_COMPLETED = 3600
    }
}
