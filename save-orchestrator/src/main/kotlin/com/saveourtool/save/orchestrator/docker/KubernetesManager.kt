package com.saveourtool.save.orchestrator.docker

import com.github.dockerjava.api.DockerClient
import com.saveourtool.save.orchestrator.DOCKER_METRIC_PREFIX
import com.saveourtool.save.orchestrator.execTimed
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import io.fabric8.kubernetes.client.KubernetesClient
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.annotation.PreDestroy

/**
 * A component that manages save-agents running in Kubernetes.
 */
@Component
@Profile("kubernetes")
class KubernetesManager(
    private val dockerClient: DockerClient,
    private val kc: KubernetesClient,
    private val meterRegistry: MeterRegistry,
): AgentRunner {

    /**
     * Cleanup resources related to the connection to the Kubernetes API server
     */
    @PreDestroy
    fun close() {
        kc.close()
    }

    @Suppress("TOO_LONG_FUNCTION")
    override fun create(executionId: Long,
                        baseImageId: String,
                        replicas: Int,
                        workingDir: String,
                        agentRunCmd: String,
    ): List<String> {
        // fixme: pass image name instead of ID from the outside
        val baseImage = dockerClient.listImagesCmd().execTimed(meterRegistry, "$DOCKER_METRIC_PREFIX.image.list").find {
            // fixme: sometimes createImageCmd returns short id without prefix, sometimes full and with prefix.
            it.id.replaceFirst("sha256:", "").startsWith(baseImageId.replaceFirst("sha256:", ""))
        }
            ?: error("Image with requested baseImageId=$baseImageId is not present in the system")
        val baseImageName = baseImage.repoTags.first()

        // Creating Kubernetes objects that will be responsible for lifecycle of save-agents.
        // We use Job, because Deployment will always try to restart failing pods.
        val job = Job().apply {
            metadata = ObjectMeta().apply {
                name = jobNameForExecution(executionId)
            }
            spec = JobSpec().apply {
                parallelism = replicas
                // do not attempt to restart failed pods, because if we manually stop pods by deleting them,
                // job controller would think that they need to be restarted
                backoffLimit = 0
                template = PodTemplateSpec().apply {
                    spec = PodSpec().apply {
                        containers = listOf(
                            Container().apply {
                                name = "save-agent-pod"
                                metadata = ObjectMeta().apply {
                                    labels = mapOf(
                                        "executionId" to "$executionId",
                                        "baseImageId" to baseImageId,
//                                        "baseImageName" to baseImageName
                                    )
                                }
                                image = baseImageName
                                imagePullPolicy = "IfNotPresent"  // so that local images could be used
                                // If agent fails, we should handle it manually (update statuses, attempt restart etc)
                                restartPolicy = "Never"
                                env = listOf(
                                    EnvVar().apply {
                                        name = "POD_NAME"
                                        valueFrom = EnvVarSource().apply {
                                            fieldRef = ObjectFieldSelector().apply {
                                                fieldPath = "metadata.name"
                                            }
                                        }
                                    }
                                )
                                command = agentRunCmd.split(" ")
                                this.workingDir = workingDir
                            }
                        )
                    }
                }
            }
        }
        kc.batch().v1().jobs().create(job)
        // todo: do we need to wait for pods to be created?
        return kc.pods().withLabel("baseImageId", baseImageId).list().items.map { it.metadata.name }
    }

    override fun start(executionId: Long) {
        logger.debug("${this::class.simpleName}#start is called, but it's no-op because Kubernetes workloads are managed by Kubernetes itself")
    }

    override fun stop(executionId: Long) {
        val jobName = jobNameForExecution(executionId)
        val isDeleted = kc.batch().v1().jobs().withName(jobName).delete()
        if (!isDeleted) throw AgentRunnerException("Failed to delete job with name $jobName")
    }

    override fun stopByAgentId(agentId: String): Boolean {
        val isDeleted = kc.pods().withName(agentId).delete()
        if (!isDeleted) throw AgentRunnerException("Failed to delete pod with name $agentId")
        else return true
    }

    override fun cleanup(executionId: Long) {
        TODO("Not yet implemented")
    }

    private fun jobNameForExecution(executionId: Long) = "save-execution-$executionId"

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesManager::class.java)
    }
}
