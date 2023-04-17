/**
 * Utils for kubernetes client
 */

package com.saveourtool.save.demo.utils

import com.saveourtool.save.demo.DemoAgentConfig
import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.config.KubernetesConfig
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.storage.DemoInternalFileStorage
import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.downloadAndRunAgentCommand
import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientException
import io.fabric8.kubernetes.client.dsl.ScalableResource
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory

private const val DEMO_ORG_NAME = "organizationName"
private const val DEMO_PROJ_NAME = "projectName"
private const val DEMO_VERSION = "version"
private const val REPLICAS_PER_DEMO = 1
private const val TTL_AFTER_COMPLETED = 3600

private const val SHA1_PREFIX_SIZE = 6

private val logger = LoggerFactory.getLogger("KubernetesUtils")

/**
 * Create kubernetes resource and process possible exceptions
 *
 * @param resourceToCreate resource (e.g. [Job], [Service] etc) that should be created by [KubernetesClient]
 * @return created [HasMetadata] resource
 * @throws KubernetesRunnerException on failed job creation
 */
fun <T : HasMetadata> KubernetesClient.createResourceOrThrow(resourceToCreate: T): T = try {
    logger.debug { "Attempt to create ${resourceToCreate.kind} with the following name: ${resourceToCreate.fullResourceName}" }
    resource(resourceToCreate).create().also {
        logger.info("Created ${resourceToCreate.kind} with the following name: ${resourceToCreate.fullResourceName}")
    }
} catch (kex: KubernetesClientException) {
    throw KubernetesRunnerException("Unable to create ${resourceToCreate.kind} with the following name: ${resourceToCreate.fullResourceName}", kex)
}

/**
 * @param demo demo entity
 * @return [ScalableResource] of [Job]
 */
fun KubernetesClient.getJobByName(demo: Demo): ScalableResource<Job> = batch()
    .v1()
    .jobs()
    .withName(jobNameForDemo(demo))

private fun ContainerPort.default(port: Int) = apply {
    protocol = "TCP"
    containerPort = port
    name = "agent-server"
}

/**
 * @param demo demo entity
 * @param agentDownloadUrl url to download save-demo-agent.kexe, will be used to get pod start command
 * @param kubernetesSettings kubernetes configuration
 * @param agentConfig configuration that is required to be present on save-demo-agent on startup
 * @return [Job], filled with all the required info, but not created yet
 * @see createResourceOrThrow
 */
@Suppress("NestedBlockDepth")
fun getJobObjectForDemo(
    demo: Demo,
    agentDownloadUrl: String,
    kubernetesSettings: KubernetesConfig,
    agentConfig: ConfigProperties.AgentConfig,
) = Job().apply {
    metadata = ObjectMeta().apply {
        name = jobNameForDemo(demo)
        namespace = kubernetesSettings.agentNamespace
    }
    spec = JobSpec().apply {
        parallelism = REPLICAS_PER_DEMO
        ttlSecondsAfterFinished = TTL_AFTER_COMPLETED
        backoffLimit = 0
        template = PodTemplateSpec().apply {
            spec = PodSpec().apply {
                subdomain = kubernetesSettings.agentSubdomainName
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
                restartPolicy = "Never"
                containers = listOf(
                    demoAgentContainerSpec(
                        demo.sdk.toSdk().baseImageName(),
                        agentDownloadUrl,
                        demo,
                        kubernetesSettings,
                        agentConfig,
                    )
                )
            }
        }
    }
}

/**
 * @param demo [Demo] entity
 * @param job already created job for ownerResource setting
 * @param kubernetesSettings kubernetes configuration
 * @return [ServicePort], filled with all the required info, but not created yet
 * @see createResourceOrThrow
 */
@Suppress("NestedBlockDepth")
fun getServiceObjectForDemo(
    demo: Demo,
    job: HasMetadata,
    kubernetesSettings: KubernetesConfig,
) = Service().apply {
    metadata = ObjectMeta().apply {
        name = serviceNameForDemo(demo)
        namespace = kubernetesSettings.agentNamespace
    }

    addOwnerReference(job)

    spec = ServiceSpec().apply {
        type = "ClusterIP"
        ports = listOf(
            ServicePort().apply {
                protocol = "TCP"
                port = kubernetesSettings.agentPort
                targetPort = IntOrString(kubernetesSettings.agentPort)
                name = "web-server"
            }
        )
        selector = mapOf(
            DEMO_ORG_NAME to demo.organizationName,
            DEMO_PROJ_NAME to demo.projectName,
            "io.kompose.service" to "save-demo-agent",
        )
    }
}

/**
 * @param demo demo entity
 * @return name of job that is/should be assigned to [demo]
 */
fun jobNameForDemo(demo: Demo) = with(demo) {
    // sha1 is required due to kubernetes naming restrictions - no capital letters are allowed while in save-cloud it is ok
    val sha1 = DigestUtils.sha1Hex("$organizationName$projectName")
    "demo-${organizationName.lowercase()}-${projectName.lowercase()}-${ sha1.take(SHA1_PREFIX_SIZE) }"
}

/**
 * @param demo demo entity
 * @return name of service that is/should be connected with [demo]
 */
fun serviceNameForDemo(demo: Demo) = with(demo) {
    // sha1 is required due to kubernetes naming restrictions - no capital letters are allowed while in save-cloud it is ok
    val sha1 = DigestUtils.sha1Hex("$organizationName$projectName")
    "demo-${organizationName.lowercase().first()}${projectName.lowercase().first()}-${ sha1.take(SHA1_PREFIX_SIZE) }"
}

@Suppress("SameParameterValue")
private fun getConfigureMeUrl(baseUrl: String, demo: Demo, version: String) = with(demo) {
    "$baseUrl/demo/internal/manager/$organizationName/$projectName/configure-me?version=$version"
}

@Suppress("TOO_LONG_FUNCTION")
private fun demoAgentContainerSpec(
    imageName: String,
    agentDownloadUrl: String,
    demo: Demo,
    kubernetesSettings: KubernetesConfig,
    agentConfig: ConfigProperties.AgentConfig,
) = Container().apply {
    name = "save-demo-agent-pod"
    image = imageName
    imagePullPolicy = "IfNotPresent"

    // todo: in later phases should be removed
    val currentlyHardcodedVersion = "manual"
    listOf(
        "KTOR_LOG_LEVEL" to "DEBUG",
        DemoAgentConfig.DEMO_CONFIGURE_ME_URL_ENV to getConfigureMeUrl(agentConfig.demoUrl, demo, currentlyHardcodedVersion),
        DemoAgentConfig.DEMO_ORGANIZATION_ENV to demo.organizationName,
        DemoAgentConfig.DEMO_PROJECT_ENV to demo.projectName,
        DemoAgentConfig.DEMO_VERSION_ENV to currentlyHardcodedVersion,
    )
        .map { (key, envValue) ->
            EnvVar().apply {
                name = key
                value = envValue
            }
        }
        .let { env = it }

    val startupCommand = downloadAndRunAgentCommand(
        agentDownloadUrl, DemoInternalFileStorage.saveDemoAgent,
    )

    command = listOf("sh", "-c", startupCommand)

    ports = listOf(ContainerPort().default(kubernetesSettings.agentPort))

    resources = with(kubernetesSettings) {
        ResourceRequirements().apply {
            requests = mapOf(
                "cpu" to Quantity(agentCpuRequests),
                "memory" to Quantity(agentMemoryRequests),
                "ephemeral-storage" to Quantity(agentEphemeralStorageRequests),
            )
            limits = mapOf(
                "cpu" to Quantity(agentCpuLimits),
                "memory" to Quantity(agentMemoryLimits),
                "ephemeral-storage" to Quantity(agentEphemeralStorageLimits),
            )
        }
    }
}
