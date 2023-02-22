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
    val agentConfig: AgentConfig,
    val s3Storage: S3OperationsProperties,
    val kubernetes: KubernetesConfig?,
) {
    /**
     * @property demoUrl url of save-demo
     */
    data class AgentConfig(
        val demoUrl: String
    )
}

/**
 * `m` stands for milli, `M` stands for Mega.
 * By default, `M` and `m` are powers of 10.
 * To be more accurate and use `M` as 1024 instead of 1000, `i` should be provided: `Mi`
 * https://kubernetes.io/docs/reference/kubernetes-api/common-definitions/quantity/
 *
 * @property apiServerUrl
 * @property serviceAccount
 * @property namespace
 * @property useGvisor
 * @property agentCpuRequests
 * @property agentCpuLimits
 * @property agentMemoryRequests
 * @property agentMemoryLimits
 */
data class KubernetesConfig(
    val apiServerUrl: String,
    val serviceAccount: String,
    val namespace: String,
    val useGvisor: Boolean,
    val agentCpuRequests: String = "100m",
    val agentCpuLimits: String = "500m",
    val agentMemoryRequests: String = "300Mi",
    val agentMemoryLimits: String = "500Mi",
)
