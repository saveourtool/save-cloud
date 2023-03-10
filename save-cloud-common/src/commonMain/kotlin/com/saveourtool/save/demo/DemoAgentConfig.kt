/**
 * Configuration data classes
 */

package com.saveourtool.save.demo

import com.saveourtool.save.utils.DEFAULT_SETUP_SH_TIMEOUT_MILLIS
import kotlinx.serialization.Serializable

/**
 * Data class that contains everything for save-demo-agent configuration
 *
 * @property demoConfiguration all the information about current demo e.g. maintainer and version
 * @property runConfiguration all the required information to run demo
 * @property demoUrl url of save-demo
 * @property setupShTimeoutMillis amount of milliseconds to run setup.sh if it is present, [DEFAULT_SETUP_SH_TIMEOUT_MILLIS] by default
 */
@Serializable
data class DemoAgentConfig(
    val demoUrl: String,
    val demoConfiguration: DemoConfiguration,
    val runConfiguration: RunConfiguration,
    val setupShTimeoutMillis: Long = DEFAULT_SETUP_SH_TIMEOUT_MILLIS,
) {
    companion object {
        const val DEMO_CONFIGURE_ME_URL_ENV = "SAVE_DEMO_CONFIGURE_ME_URL"
        const val DEMO_ORGANIZATION_ENV = "SAVE_DEMO_ORGANIZATION_ENV"
        const val DEMO_PROJECT_ENV = "SAVE_DEMO_PROJECT_ENV"
        const val DEMO_VERSION_ENV = "SAVE_DEMO_VERSION_ENV"
    }
}

/**
 * Data class that contains the information that is used for demo file fetch
 *
 * @property organizationName name of organization that runs the demo
 * @property projectName name of project that runs the demo
 * @property version current version of demo
 */
@Serializable
data class DemoConfiguration(
    val organizationName: String,
    val projectName: String,
    val version: String = "manual",
)

/**
 * Data class that contains all the required information to run demo
 *
 * @property inputFileName name of input file name
 * @property configFileName name of config file or null if not supported
 * @property runCommand command that should be executed in order to launch demo run
 * @property outputFileName name of a file that contains the output information e.g. report
 * @property logFileName name of file that contains logs
 */
@Serializable
data class RunConfiguration(
    val inputFileName: String,
    val configFileName: String?,
    val runCommand: String,
    val outputFileName: String?,
    val logFileName: String = "logs.txt",
)

/**
 * Data class that contains all the required information to start save-demo-agent server
 *
 * @property port port number that server should run on, 23456 by default
 */
@Serializable
data class ServerConfiguration(
    val port: Long = 23456L,
)
