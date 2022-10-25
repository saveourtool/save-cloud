/**
 * Classes for configuration properties of orchestrator
 */

package com.saveourtool.save.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Class for properties
 *
 * @property backendUrl url of save-backend
 * @property testResources configuration for test resources
 * @property docker configuration for docker API
 * @property kubernetes configuration for setup in Kubernetes
 * @property dockerResourcesLifetime time, after which resources (images, containers, etc) should be released
 * @property agentsCount a number of agents to start for every [Execution]
 * @property shutdown configuration related to process of shutting down groups of agents for executions
 * @property aptExtraFlags additional flags that will be passed to `apt-get` when building image for tests
 * @property adjustResourceOwner whether Linux user that will be set as owner of resources copied into docker build directory
 * @property agentsHeartBeatTimeoutMillis interval in milliseconds, after which agent should be marked as crashed, if there weren't received heartbeats from him
 * @property heartBeatInspectorInterval interval in seconds, with the frequency of which heartbeat inspector will look for crashed agents
 * @property agentSettings if set, this will override defaults in agent.properties
 * @property agentsStartTimeoutMillis interval in milliseconds, which indicates how much time is given to agents for starting, if time's up - mark execution with internal error
 * @property agentsStartCheckIntervalMillis interval in milliseconds, within which agents will be checked, whether they are started
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "orchestrator")
data class ConfigProperties(
    val backendUrl: String,
    val testResources: TestResources,
    val docker: DockerSettings?,
    val kubernetes: KubernetesSettings?,
    val dockerResourcesLifetime: String = "720h",
    val agentsCount: Int,
    val shutdown: ShutdownSettings,
    val aptExtraFlags: String = "",
    val adjustResourceOwner: Boolean = true,
    val agentsHeartBeatTimeoutMillis: Long,
    val heartBeatInspectorInterval: Long,
    val agentSettings: AgentSettings = AgentSettings(),
    val agentsStartTimeoutMillis: Long,
    val agentsStartCheckIntervalMillis: Long,
) {
    /**
     * @property tmpPath Path to the directory, where test resources can be copied into when creating volumes with test resources.
     * Because a new volume can't be mounted to the running container (in this case, save-orchestrator), and to be able to fill
     * the created volume with resources, we need to use an intermediate container, which will start with that new volume mounted.
     * To be able to access resources, orchestrator and this intermediate container should have a shared mount, and [tmpPath] serves
     * as a host location for this shared mount.
     */
    data class TestResources(
        val tmpPath: String = "/tmp",
    )

    /**
     * @property host hostname of docker daemon
     * @property runtime OCI compliant runtime for docker
     * @property loggingDriver logging driver for the container
     * @property registry docker registry to pull images for test executions from
     * @property testResourcesVolumeType Type of Docker volume (bind/volume). `bind` should only be used for local running and for tests.
     * @property testResourcesVolumeName Name of a Docker volume which acts as a temporary storage of resources for execution.
     * Nullable, because it's not required in Kubernetes
     */
    data class DockerSettings(
        val host: String,
        val loggingDriver: String,
        val runtime: String? = null,
        val registry: String = "docker.io/library",
        val testResourcesVolumeType: String = "volume",
        val testResourcesVolumeName: String? = null,
    )

    /**
     * @property apiServerUrl URL of Kubernetes API Server. See [docs on accessing API from within a pod](https://kubernetes.io/docs/tasks/run-application/access-api-from-pod/)
     * @property serviceAccount Name of [ServiceAccount](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/) that will be used
     * to authenticate orchestrator to the API server
     * @property namespace Kubernetes namespace, into which agents will be deployed.
     * @property useGvisor if true, will try to use gVisor's runsc runtime for starting agents
     * @property agentCpuRequests configures `resources.requests.cpu` for agent pods
     * @property agentCpuLimits configures `resources.limits.cpu` for agent pods
     * @property agentMemoryRequests configures `resources.requests.memory` for agent pods
     * @property agentMemoryLimits configures `resources.requests.memory` for agent pods
     */
    data class KubernetesSettings(
        val apiServerUrl: String,
        val serviceAccount: String,
        val namespace: String,
        val useGvisor: Boolean,
        val agentCpuRequests: String = "100m",
        val agentCpuLimits: String = "500m",
        val agentMemoryRequests: String = "300Mi",
        val agentMemoryLimits: String = "500Mi",
    )

    /**
     * @property backendUrl url of save-backend that will be used by save-agent
     * @property orchestratorUrl url of save-orchestrator that will be used by save-agent
     * @property debug whether debug logging should be enabled or not
     */
    data class AgentSettings(
        val backendUrl: String? = null,
        val orchestratorUrl: String? = null,
        val debug: Boolean? = null,
    )

    /**
     * @property checksIntervalMillis interval between checks whether agents are really finished
     * @property gracefulTimeoutSeconds if agent doesn't shut down during this time, it will be forcefully terminated
     * @property gracefulNumChecks during [gracefulTimeoutSeconds], perform this number of checks whether agent is still running
     */
    data class ShutdownSettings(
        val checksIntervalMillis: Long,
        val gracefulTimeoutSeconds: Long,
        val gracefulNumChecks: Int,
    )
}
