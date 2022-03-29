/**
 * Logic for configuration for http client and evaluated tool
 */

package org.cqfn.save.api

import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

private val log = LoggerFactory.getLogger(PropertiesConfiguration::class.java)

/**
 * Available types of configurations
 */
enum class PropertiesConfigurationType {
    EVALUATED_TOOL,
    WEB_CLIENT,
    ;
}

/**
 * Base class for configuration
 */
sealed class PropertiesConfiguration

/**
 * @property backendUrl
 * @property preprocessorUrl
 * @property fileStorage
 */
data class WebClientProperties(
    val backendUrl: String,
    val preprocessorUrl: String,
    val fileStorage: String,
) : PropertiesConfiguration()

/**
 * @property organizationName
 * @property projectName
 * @property gitUrl
 * @property gitUserName
 * @property gitPassword
 * @property branch
 * @property commitHash
 * @property testRootPath
 * @property additionalFiles
 * @property testSuites
 * @property execCmd
 * @property batchSize
 */
// TODO: configure sdk
data class EvaluatedToolProperties(
    val organizationName: String,
    val projectName: String,
    val gitUrl: String,
    val gitUserName: String? = null,
    val gitPassword: String? = null,
    val branch: String? = null,
    val commitHash: String?,
    val testRootPath: String,
    val additionalFiles: String? = null,
    val testSuites: String,
    val execCmd: String? = null,
    val batchSize: String? = null,
) : PropertiesConfiguration()

/**
 * @param configFileName
 * @param type
 * @return corresponding configuration
 */
@Suppress("TOO_LONG_FUNCTION")
fun readPropertiesFile(configFileName: String, type: PropertiesConfigurationType): PropertiesConfiguration? {
    try {
        val properties = Properties()
        val classLoader = AutomaticTestInitializator::class.java.classLoader
        val input = classLoader.getResourceAsStream(configFileName)
        input ?: run {
            log.error("Unable to find configuration file: $configFileName")
            return null
        }
        properties.load(input)
        when (type) {
            PropertiesConfigurationType.WEB_CLIENT -> return WebClientProperties(
                properties.getProperty("backendUrl"),
                properties.getProperty("preprocessorUrl"),
                properties.getProperty("fileStorage"),
            )
            PropertiesConfigurationType.EVALUATED_TOOL -> return EvaluatedToolProperties(
                properties.getProperty("organizationName"),
                properties.getProperty("projectName"),
                properties.getProperty("gitUrl"),
                properties.getProperty("gitUserName"),
                properties.getProperty("gitPassword"),
                properties.getProperty("branch"),
                properties.getProperty("commitHash"),
                properties.getProperty("testRootPath"),
                properties.getProperty("additionalFiles"),
                properties.getProperty("testSuites"),
                properties.getProperty("execCmd"),
                properties.getProperty("batchSize"),
            )
            else -> {
                log.error("Type $type for properties configuration doesn't supported!")
                return null
            }
        }
    } catch (ex: IOException) {
        ex.printStackTrace()
        return null
    }
}
