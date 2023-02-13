/**
 * save-demo configuration
 */

package com.saveourtool.save.demo.config

import com.saveourtool.save.s3.S3OperationsProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * @property s3Storage configuration of S3 storage
 * @property backend
 * @property kubernetes
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "demo")
data class ConfigProperties(
    val s3Storage: S3OperationsProperties,
    val kubernetes: KubernetesConfig,
    val backend: String,
)

/**
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
