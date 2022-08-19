package com.saveourtool.save.orchestrator.kubernetes

import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.runner.AgentRunner
import com.saveourtool.save.orchestrator.runner.AgentRunnerException
import com.saveourtool.save.orchestrator.runner.EXECUTION_DIR
import com.saveourtool.save.orchestrator.runner.SAVE_AGENT_USER_HOME
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.service.PersistentVolumeId
import com.saveourtool.save.utils.debug

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import io.fabric8.kubernetes.client.KubernetesClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * A component that manages save-agents running in Kubernetes.
 */
@Component
@Profile("kubernetes")
class KubernetesManager(
    private val kc: KubernetesClient,
    private val configProperties: ConfigProperties,
) : AgentRunner {
    private val boundPvcs: ConcurrentMap<Long, String> = ConcurrentHashMap()

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
    ): List<String> {
        val baseImageTag = configuration.imageTag
        val agentRunCmd = configuration.runCmd
        val pvId = configuration.pvId
        val workingDir = configuration.workingDir
        require(pvId is KubernetesPvId) { "${KubernetesPersistentVolumeService::class.simpleName} can only operate with ${KubernetesPvId::class.simpleName}" }
        requireNotNull(configProperties.kubernetes)
        // fixme: pass image name instead of ID from the outside

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
                        if (configProperties.kubernetes.useGvisor) {
                            nodeSelector = mapOf(
                                "gvisor" to "enabled"
                            )
                            runtimeClassName = "gvisor"
                        }
                        // FixMe: Orchestrator uses hostPath mounts to copy resources, so agents have to be run on the same host.
                        nodeName = System.getenv("NODE_NAME")
                        metadata = ObjectMeta().apply {
                            labels = mapOf(
                                "executionId" to executionId.toString(),
                                // "baseImageName" to baseImageName
                                "io.kompose.service" to "save-agent"
                            )
                        }
                        // If agent fails, we should handle it manually (update statuses, attempt restart etc.)
                        restartPolicy = "Never"
                        initContainers = initContainersSpec(pvId)
                        containers = listOf(
                            agentContainerSpec(baseImageTag, agentRunCmd, workingDir, configuration.resourcesConfiguration)
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
        boundPvcs[executionId] = pvId.pvcName
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
        boundPvcs.remove(executionId)?.let { pvcName ->
            logger.debug("Removing a PVC for execution id=$executionId with name $pvcName")
            kc.persistentVolumeClaims()
                .withName(pvcName)
                .delete()
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

    private fun initContainersSpec(pvId: KubernetesPvId): List<Container> {
        requireNotNull(configProperties.kubernetes)

        // FixMe: After #958 is merged we can start downloading tests directly from backend/storage into a volume.
        // Probably, a separate client process should be introduced. Until then, one init container performs copying
        // into a shared mount while others are sleeping for this many seconds:
        @Suppress(
            "FLOAT_IN_ACCURATE_CALCULATIONS",
            "MAGIC_NUMBER",
            "MagicNumber",
        )
        val waitForCopySeconds = (configProperties.agentsStartTimeoutMillis * 0.8 / 1000).toLong()

        return listOf(
            Container().apply {
                name = "save-vol-copier"
                image = "alpine:latest"
                val targetDir = configProperties.kubernetes.pvcMountPath
                command = listOf(
                    "sh", "-c",
                    "if [ -z \"$(ls -A $targetDir)\" ];" +
                            " then mkdir -p $targetDir && cp -R ${pvId.sourcePath}/* $targetDir" +
                            " && chown -R 1100:1100 $targetDir && echo Successfully copied;" +
                            " else echo Copying already in progress && ls -A $targetDir && sleep $waitForCopySeconds;" +
                            " fi"
                )
                volumeMounts = listOf(
                    VolumeMount().apply {
                        name = "save-resources-tmp"
                        mountPath = "$SAVE_AGENT_USER_HOME/tmp"
                    },
                    VolumeMount().apply {
                        name = "save-execution-pvc"
                        mountPath = configProperties.kubernetes.pvcMountPath
                    }
                )
            }
        )
    }

    @Suppress("TOO_LONG_FUNCTION")
    private fun agentContainerSpec(
        imageName: String,
        agentRunCmd: List<String>,
        workingDir: String,
        resourcesConfiguration: DockerService.RunConfiguration.ResourcesConfiguration,
    ) = Container().apply {
        name = "save-agent-pod"
        image = imageName
        imagePullPolicy = "IfNotPresent"  // so that local images could be used
        env = listOf(
            EnvVar().apply {
                name = "POD_NAME"
                valueFrom = EnvVarSource().apply {
                    fieldRef = ObjectFieldSelector().apply {
                        fieldPath = "metadata.name"
                    }
                }
            },
            EnvVar().apply {
                name = "EXECUTION_ID"
                value = "${resourcesConfiguration.executionId}"
            },
            EnvVar().apply {
                name = "ADDITIONAL_FILES_LIST"
                value = resourcesConfiguration.additionalFilesSting
            },
            EnvVar().apply {
                name = "ORGANIZATION_NAME"
                value = resourcesConfiguration.organizationName
            },
            EnvVar().apply {
                name = "PROJECT_NAME"
                value = resourcesConfiguration.projectName
            },
        )

        val resourcesPath = requireNotNull(configProperties.kubernetes).pvcMountPath
        this.command = agentRunCmd.dropLast(1)
        this.args = listOf("cp $resourcesPath/* . && ${agentRunCmd.last()}")

        this.workingDir = workingDir
        volumeMounts = listOf(
            VolumeMount().apply {
                name = "save-execution-pvc"
                mountPath = resourcesPath
            }
        )
    }

    private fun kcJobsWithName(name: String) = kc.batch()
        .v1()
        .jobs()
        .withName(name)

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesManager::class.java)
    }
}
