/**
 * Classes for configuration properties of orchestrator
 */

package org.cqfn.save.orchestrator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Class for properties
 *
 * @property backendUrl url of save-backend
 * @property testResources configuration for test resources
 * @property docker configuration for docker API
 * @property agentsCount a number of agents to start for every [Execution]
 * @property executionLogs path to folder to store cli logs
 * @property shutdownChecksIntervalMillis interval between checks whether agents are really finished
 * @property aptExtraFlags additional flags that will be passed to `apt-get` when building image for tests
 * @property adjustResourceOwner whether Linux user that will be set as owner of resources copied into docker build directory
 * @property agentsHeartBeatTimeoutMillis interval in milliseconds, after which agent should be marked as crashed, if there weren't received heartbeats from him
 * @property heartBeatInspectorInterval interval in seconds, with the frequency of which heartbeat inspector will look for crashed agents
 * @property agentSettings if set, this will override defaults in agent.properties
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "orchestrator")
data class ConfigProperties(
    val backendUrl: String,
    val testResources: TestResources,
    val docker: DockerSettings?,
    val kubernetes: KubernetesSettings?,
    val agentsCount: Int,
    val executionLogs: String,
    val shutdownChecksIntervalMillis: Long,
    val aptExtraFlags: String = "",
    val adjustResourceOwner: Boolean = true,
    val agentsHeartBeatTimeoutMillis: Long,
    val heartBeatInspectorInterval: Long,
    val agentSettings: AgentSettings = AgentSettings(),
) {
    init {
        require(docker != null || kubernetes != null) {
            "Either docker or kubernetes config should be provided in application properties, but both are missing."
        }
    }
}

/**
 * @property basePath path to the root directory, where all test resources are stored
 */
data class TestResources(
    val basePath: String,
)

/**
 * @property host hostname of docker daemon
 * @property runtime OCI compliant runtime for docker
 * @property loggingDriver logging driver for the container
 */
data class DockerSettings(
    val host: String,
    val loggingDriver: String,
    val runtime: String = "runc",
)

data class KubernetesSettings(
    val apiServerUrl: String,
    val namespace: String,
)

/**
 * @property backendUrl url of save-backend that will be used by save-agent
 * @property orchestratorUrl url of save-orchestrator that will be used by save-agent
 */
data class AgentSettings(
    val backendUrl: String? = null,
    val orchestratorUrl: String? = null,
)
