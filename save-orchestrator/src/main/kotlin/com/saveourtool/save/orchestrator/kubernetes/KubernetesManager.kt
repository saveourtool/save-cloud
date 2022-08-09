package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.findImage
import com.saveourtool.save.orchestrator.runner.AgentRunner
import com.saveourtool.save.orchestrator.runner.AgentRunnerException
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.runner.SAVE_AGENT_USER_HOME
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.service.PersistentVolumeId
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.warn

import com.github.dockerjava.api.DockerClient
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import io.fabric8.kubernetes.client.KubernetesClient
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * A component that manages save-agents running in Kubernetes.
 */
@Component
@Profile("kubernetes")
class KubernetesManager(
    private val dockerClient: DockerClient,
    private val kc: KubernetesClient,
    private val configProperties: ConfigProperties,
    private val meterRegistry: MeterRegistry,
) : AgentRunner {
    @Suppress(
        "TOO_LONG_FUNCTION",
        "LongMethod",
        "MagicNumber",
        "NestedBlockDepth",
        "ComplexMethod",
    )
    override fun create(executionId: Long,
                        configuration: DockerService.RunConfiguration<PersistentVolumeId>,
                        replicas: Int,
                        workingDir: String,
    ): List<String> {
        val (baseImageId, agentRunCmd, pvId) = configuration
        require(pvId is KubernetesPvId) { "${KubernetesPersistentVolumeService::class.simpleName} can only operate with ${KubernetesPvId::class.simpleName}" }
        // fixme: pass image name instead of ID from the outside
        val baseImage = dockerClient.findImage(baseImageId, meterRegistry)
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
                        if (configProperties.kubernetes?.useGvisor == true) {
                            nodeSelector = mapOf(
                                "gvisor" to "enabled"
                            )
                        }
                        // FixMe: Orchestrator doesn't push images to a remote registry, so agents have to be run on the same host.
                        nodeName = System.getenv("NODE_NAME")
                        initContainers = listOf(
                            Container().apply {
                                // FixMe: After #958 is merged we can start downloading tests directly from backend/storage into a volume.
                                // Probably, a separate client process should be introduced. Until then, one init container performs copying
                                // into a shared mount while others are sleeping for this many seconds:
                                @Suppress("FLOAT_IN_ACCURATE_CALCULATIONS", "MAGIC_NUMBER")
                                val waitForCopySeconds = (configProperties.agentsStartTimeoutMillis * 0.8 / 1000).toLong()
                                name = "save-vol-copier"
                                image = "alpine:latest"
                                command = listOf(
                                    "sh", "-c",
                                    "if [ -z \"$(ls -A $EXECUTION_DIR)\" ];" +
                                            " then mkdir -p $EXECUTION_DIR && cp -R ${pvId.sourcePath}/* $EXECUTION_DIR" +
                                            " && chown -R 1100:1100 $EXECUTION_DIR && echo Successfully copied;" +
                                            " else echo Copying already in progress && ls -A $EXECUTION_DIR && sleep $waitForCopySeconds;" +
                                            " fi"
                                )
                                volumeMounts = listOf(
                                    VolumeMount().apply {
                                        name = "save-resources-tmp"
                                        mountPath = "$SAVE_AGENT_USER_HOME/tmp"
                                    },
                                    VolumeMount().apply {
                                        name = "save-execution-pvc"
                                        mountPath = configProperties.kubernetes!!.pvcMountPath
                                    }
                                )
                            }
                        )
                        containers = listOf(
                            Container().apply {
                                name = "save-agent-pod"
                                metadata = ObjectMeta().apply {
                                    labels = mapOf(
                                        "executionId" to executionId.toString(),
                                        // "baseImageName" to baseImageName
                                        "io.kompose.service" to "save-agent"
                                    )
                                }
                                image = baseImageName
                                imagePullPolicy = "IfNotPresent"  // so that local images could be used
                                // If agent fails, we should handle it manually (update statuses, attempt restart etc.)
                                restartPolicy = "Never"
                                if (!configProperties.docker.runtime.isNullOrEmpty()) {
                                    logger.warn {
                                        "Discarding property configProperties.docker.runtime=${configProperties.docker.runtime}, " +
                                                "because custom runtimes are not supported yet"
                                    }
                                }
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

                                // `agentRunCmd` looks like `sh -c "rest of the command"`
                                val (command, args) = agentRunCmd
                                    .let {
                                        it.substringBefore('"').trim().split(" ") + "\"${it.substringAfter('"')}"
                                    }
                                    .let {
                                        it.dropLast(1) to it.last().trim('"')
                                    }
                                this.command = command
                                this.args = listOf(args)

                                this.workingDir = workingDir
                                volumeMounts = listOf(
                                    VolumeMount().apply {
                                        name = "save-execution-pvc"
                                        mountPath = configProperties.kubernetes!!.pvcMountPath
                                    }
                                )
                            }
                        )
                        volumes = listOf(
                            Volume().apply {
                                name = "save-resources-tmp"
                                persistentVolumeClaim = PersistentVolumeClaimVolumeSource().apply {
                                    claimName = pvId.sourcePvcName
                                }
                            },
                            Volume().apply {
                                name = "save-execution-pvc"
                                persistentVolumeClaim = PersistentVolumeClaimVolumeSource().apply {
                                    claimName = pvId.pvcName
                                }
                            }
                        )
                    }
                }
            }
        }
        logger.debug { "Attempt to create Job from the following spec: $job" }
        kc.resource(job)
            .create()
        logger.info("Created Job for execution id=$executionId")
        // fixme: wait for pods to be created
        return generateSequence<List<String>> {
            Thread.sleep(1_000)
            kc.pods().withLabel("executionId", executionId.toString())
                .list()
                .items
                .map { it.metadata.name }
        }
            .take(10)
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
    }

    override fun start(executionId: Long) {
        logger.debug { "${this::class.simpleName}#start is called, but it's no-op because Kubernetes workloads are managed by Kubernetes itself" }
    }

    override fun stop(executionId: Long) {
        val jobName = jobNameForExecution(executionId)
        val deletedResources = kcJobsWithName(jobName)
            .delete()
        val isDeleted = deletedResources.size == 1
        if (!isDeleted) {
            throw AgentRunnerException("Failed to delete job with name $jobName: response is $deletedResources")
        }
        logger.debug("Deleted Job for execution id=$executionId")
    }

    override fun stopByAgentId(agentId: String): Boolean {
        val pod: Pod? = kc.pods().withName(agentId).get()
        pod ?: run {
            logger.debug("Agent id=$agentId is already stopped or not yet created")
            return true
        }
        val deletedResources = kc.pods().withName(agentId).delete()
        val isDeleted = deletedResources.size == 1
        if (!isDeleted) {
            throw AgentRunnerException("Failed to delete pod with name $agentId: response is $deletedResources")
        } else {
            logger.debug("Deleted pod with name=$agentId")
            return true
        }
    }

    override fun cleanup(executionId: Long) {
        logger.debug("Removing a Job for execution id=$executionId")
        val job = kcJobsWithName(jobNameForExecution(executionId))
        job.get()?.let {
            job.delete()
        }
    }

    override fun prune() {
        logger.debug("${this::class.simpleName}#prune is called, but it's no-op, " +
                "because we don't directly interact with the docker containers or images on the nodes of Kubernetes themselves")
    }

    override fun isAgentStopped(agentId: String): Boolean {
        val pod = kc.pods().withName(agentId).get()
        return pod == null || run {
            // Retrieve reason based on https://github.com/kubernetes/kubernetes/issues/22839
            val reason = pod.status.phase ?: pod.status.reason
            val isRunning = pod.status.containerStatuses.any {
                it.ready && it.state.running != null
            }
            logger.debug("Pod name=$agentId is still present; reason=$reason, isRunning=$isRunning, conditions=${pod.status.conditions}")
            if (reason == "Completed" && isRunning) {
                "ContainerReady" in pod.status.conditions.map { it.type }
            } else {
                !isRunning
            }
        }
    }

    private fun jobNameForExecution(executionId: Long) = "save-execution-$executionId"

    private fun kcJobsWithName(name: String) = kc.batch()
        .v1()
        .jobs()
        .withName(name)

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesManager::class.java)
    }
}
