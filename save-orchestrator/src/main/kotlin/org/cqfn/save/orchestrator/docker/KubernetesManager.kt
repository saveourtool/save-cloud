package org.cqfn.save.orchestrator.docker

import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1DeploymentSpec
import io.kubernetes.client.openapi.models.V1EnvVar
import io.kubernetes.client.openapi.models.V1EnvVarSource
import io.kubernetes.client.openapi.models.V1ObjectFieldSelector
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1PodSpec
import io.kubernetes.client.openapi.models.V1PodTemplateSpec
import io.kubernetes.client.proto.V1Apps.ControllerRevisionOrBuilder
import io.kubernetes.client.util.Config
import org.cqfn.save.orchestrator.config.ConfigProperties
import org.cqfn.save.orchestrator.config.KubernetesSettings
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * For save-agent in K8S the lifecycle is as follows:
 * create ConfigMap -> create Pod (also starts containers)
 * stop: remove Pod -> remove ConfigMap
 */
@Component
@Profile("kubernetes")
class KubernetesManager(
    configProperties: ConfigProperties,
): AgentRunner {
    private val kubernetesSettings = requireNotNull(configProperties.kubernetes) {
        "Class [${this::class.simpleName}] requires kubernetes-related properties to be set"
    }
    private val coreV1Api = CoreV1Api(
        // If Orchestrator runs inside the Kubernetes cluster, client can be configured automatically including
        //   1. service-account CA
        //   2. service-account bearer-token
        //   3. service-account namespace
        //   4. master endpoints(ip, port) from pre-set environment variables
        Config.fromCluster()
    )

    override fun create(baseImageId: String,
                        workingDir: String,
                        runCmd: String,
                        containerName: String,
    ): String? {
        V1Deployment().apply {
            metadata = V1ObjectMeta().apply {
                name = containerName
            }
            spec = V1DeploymentSpec().apply {
//                replicas =
                template = V1PodTemplateSpec().apply {
                    spec = podModel
                }
            }
        }

        val podModel = V1Pod().apply pod@{
            spec = V1PodSpec().apply {
                containers = listOf(
                    V1Container().apply {
                        image = baseImageId
                        name = containerName
                        // todo: value
                        restartPolicy = "never"
                        env = listOf(
                            V1EnvVar().apply {
                                name = "POD_NAME"
                                valueFrom = V1EnvVarSource().apply {
                                    fieldRef = V1ObjectFieldSelector().apply {
                                        fieldPath = this@pod.metadata!!.name
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }
        val pod = coreV1Api.createNamespacedPod(
            kubernetesSettings.namespace,
            podModel,
            "false",
            "false",
            "save-orchestrator-kubernetes-client",
            "Ignore"
        )
        return pod.metadata?.name
    }

    override fun start(id: String) {
        logger.debug("${this::class.simpleName}#start is called, but it's no-op because Kubernetes workloads are managed by Kubernetes itself")
    }

    override fun stop(id: String) {
        coreV1Api.deleteNamespacedPod(
            id,
            kubernetesSettings.namespace,
            "false",
            "false",
            5,
            true,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesManager::class.java)
    }
}