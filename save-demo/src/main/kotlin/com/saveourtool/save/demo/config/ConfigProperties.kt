/**
 * save-demo configuration
 */

package com.saveourtool.save.demo.config

import com.saveourtool.save.s3.S3OperationsProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property backendUrl URL of backend
 * @property agentConfig configuration of save-demo-agents that are run by save-demo
 * @property s3Storage configuration of S3 storage
 * @property kubernetes kubernetes configuration
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "demo")
data class ConfigProperties(
    val backendUrl: String,
    override val s3Storage: S3OperationsProperties,
    val kubernetes: KubernetesConfig?,
    val agentConfig: AgentConfig? = null,
) : S3OperationsProperties.Provider {
    /**
     * @property demoUrl url of save-demo
     * @property parentUserName name of a parent process user, needed for token isolation
     * @property childUserName name of a child process user, needed for token isolation
     */
    data class AgentConfig(
        val demoUrl: String,
        val parentUserName: String? = null,
        val childUserName: String? = null,
    )
}

/**
 * `m` stands for milli, `M` stands for Mega.
 * By default, `M` and `m` are powers of 10.
 * To be more accurate and use `M` as 1024 instead of 1000, `i` should be provided: `Mi`
 * [reference](https://kubernetes.io/docs/reference/kubernetes-api/common-definitions/quantity/)
 *
 * @property apiServerUrl URL of Kubernetes API Server. See [docs on accessing API from within a pod](https://kubernetes.io/docs/tasks/run-application/access-api-from-pod/)
 * @property serviceAccount Name of [ServiceAccount](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/) that will be used
 * to authenticate save-demo to the API server
 * @property currentNamespace namespace that demo works in
 * @property useGvisor if true, will try to use gVisor's runsc runtime for starting agents
 * @property agentSubdomainName name of service that is created in order to access agents
 * @property agentPort port of agent that should be used to access it
 * @property agentNamespace namespace that demo-agents should work in, [currentNamespace] by default
 * @property agentCpuRequests configures `resources.requests.cpu` for demo-agent pods
 * @property agentCpuLimits configures `resources.limits.cpu` for demo-agent pods
 * @property agentMemoryRequests configures `resources.requests.memory` for demo-agent pods
 * @property agentMemoryLimits configures `resources.limits.memory` for demo-agent pods
 * @property agentEphemeralStorageRequests configures `resources.requests.ephemeralStorage` for demo-agent pods
 * @property agentEphemeralStorageLimits configures `resources.limits.ephemeralStorage` for demo-agent pods
 */
data class KubernetesConfig(
    val apiServerUrl: String,
    val serviceAccount: String,
    val currentNamespace: String,
    val useGvisor: Boolean,
    val agentSubdomainName: String,
    val agentPort: Int,
    val agentNamespace: String = currentNamespace,
    val agentCpuRequests: String = "100m",
    val agentCpuLimits: String = "500m",
    val agentMemoryRequests: String = "300Mi",
    val agentMemoryLimits: String = "500Mi",
    val agentEphemeralStorageRequests: String = "100Mi",
    val agentEphemeralStorageLimits: String = "500Mi",
)
