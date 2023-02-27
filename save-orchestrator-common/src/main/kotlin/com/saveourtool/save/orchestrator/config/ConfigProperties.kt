/**
 * Classes for configuration properties of orchestrator
 */

package com.saveourtool.save.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

/**
 * Class for properties
 *
 * @property docker configuration for docker API
 * @property kubernetes configuration for setup in Kubernetes
 * @property dockerResourcesLifetime time, after which resources (images, containers, etc) should be released
 * @property containerNamePrefix a prefix for container name
 * @property agentsCount a number of agents to start for every [Execution]
 * @property shutdown configuration related to process of shutting down groups of agents for executions
 * @property aptExtraFlags additional flags that will be passed to `apt-get` when building image for tests
 * @property adjustResourceOwner whether Linux user that will be set as owner of resources copied into docker build directory
 * @property agentsHeartBeatTimeoutMillis interval in milliseconds, after which agent should be marked as crashed, if there weren't received heartbeats from him
 * @property heartBeatInspectorCron cron expression for heartbeat inspector to look for crashed agents
 * @property agentSettings if set, this will override defaults in agent.toml
 * @property agentsStartTimeoutMillis interval in milliseconds, which indicates how much time is given to agents for starting, if time's up - mark execution with internal error
 * @property agentsStartCheckIntervalMillis interval in milliseconds, within which agents will be checked, whether they are started
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "orchestrator")
data class ConfigProperties(
    val docker: DockerSettings?,
    val kubernetes: KubernetesSettings?,
    val dockerResourcesLifetime: String = "720h",
    val containerNamePrefix: String = "save-execution-",
    val agentsCount: Int,
    val shutdown: ShutdownSettings,
    val aptExtraFlags: String = "",
    val adjustResourceOwner: Boolean = true,
    val agentsHeartBeatTimeoutMillis: Long,
    val heartBeatInspectorCron: String,
    val agentSettings: AgentSettings,
    val agentsStartTimeoutMillis: Long,
    val agentsStartCheckIntervalMillis: Long,
) {
    /**
     * @property host hostname of docker daemon
     * @property useLoki this flag enables loki logging
     * @property runtime OCI compliant runtime for docker
     * @property registry docker registry to pull images for test executions from
     * @property testResourcesVolumeType Type of Docker volume (bind/volume). `bind` should only be used for local running and for tests.
     * @property testResourcesVolumeName Name of a Docker volume which acts as a temporary storage of resources for execution.
     * Nullable, because it's not required in Kubernetes
     */
    data class DockerSettings(
        val host: String,
        val useLoki: Boolean = true,
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
     * @property ttlAfterFinished agent job time to live after it is marked as completed
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
        val ttlAfterFinished: Duration = Duration.ofMinutes(3),
    )

    /**
     * @property heartbeatUrl url that will be used by save-agent to post heartbeats
     * @property debug whether debug logging should be enabled or not
     */
    data class AgentSettings(
        val heartbeatUrl: String,
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
