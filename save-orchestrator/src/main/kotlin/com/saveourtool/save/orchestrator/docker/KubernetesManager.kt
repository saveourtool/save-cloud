package org.cqfn.save.orchestrator.docker

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import io.fabric8.kubernetes.client.DefaultKubernetesClient
import org.cqfn.save.orchestrator.config.ConfigProperties
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
    configProperties: ConfigProperties,
): AgentRunner {
    @PreDestroy
    fun close() {
        kc.close()
    }

    private val kubernetesSettings = requireNotNull(configProperties.kubernetes) {
        "Class [${KubernetesManager::class.simpleName}] requires kubernetes-related properties to be set"
    }

    private val kc = DefaultKubernetesClient().inNamespace(kubernetesSettings.namespace)

    override fun create(executionId: Long,
                        baseImageId: String,
                        replicas: Int,
                        workingDir: String,
                        agentRunCmd: String,
    ): List<String> {
        // Creating Kubernetes objects that will be responsible for lifecycle of save-agents.
        // We use Job, because Deployment will always try to restart failing pods.
        val job = Job().apply {
            metadata = ObjectMeta().apply {
                name = jobNameForExecution("$executionId")
            }
            spec = JobSpec().apply {
                parallelism = replicas
                template = PodTemplateSpec().apply {
                    spec = PodSpec().apply {
                        containers = listOf(
                            Container().apply {
                                metadata = ObjectMeta().apply {
                                    labels = mapOf(
                                        "executionId" to "$executionId",
                                        "baseImageId" to baseImageId
                                    )
                                }
                                image = baseImageId
                                imagePullPolicy = "IfNotPresent"  // so that local images could be used
                                // If agent fails, we should handle it manually (update statuses, attempt restart etc)
                                restartPolicy = "Never"
                                env = listOf(
                                    EnvVar().apply {
                                        name = "POD_NAME"
                                        valueFrom = EnvVarSource().apply {
                                            fieldRef = ObjectFieldSelector().apply {
                                                fieldPath = "spec.name"
                                            }
                                        }
                                    }
                                )
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

    override fun stop(executionId: String) {
        val jobName = jobNameForExecution(executionId)
        val isDeleted = kc.batch().v1().jobs().withName(jobName).delete()
        if (!isDeleted) throw AgentRunnerException("Failed to delete job with name $jobName")
    }

    override fun stopByAgentId(agentId: String) {
        val isDeleted = kc.pods().withName(agentId).delete()
        if (!isDeleted) throw AgentRunnerException("Failed to delete pod with name $agentId")
    }

    override fun cleanup(executionId: Long) {
        TODO("Not yet implemented")
    }

    private fun jobNameForExecution(executionId: String) = "save-execution-$executionId"

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesManager::class.java)
    }
}
