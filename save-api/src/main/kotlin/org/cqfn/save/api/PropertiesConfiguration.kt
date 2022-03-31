/**
 * Logic for configuration for http client and evaluated tool
 */

package org.cqfn.save.api

import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

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
@Serializable
sealed class PropertiesConfiguration

/**
 * @property backendUrl
 * @property preprocessorUrl
 * @property fileStorage
 */
@Serializable
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
@Serializable
data class EvaluatedToolProperties(
    val organizationName: String,
    val projectName: String,
    val gitUrl: String,
    val gitUserName: String? = null,
    val gitPassword: String? = null,
    val branch: String? = null,
    val commitHash: String? = null,
    val testRootPath: String,
    val additionalFiles: String? = null,
    val testSuites: String,
    val execCmd: String? = null,
    val batchSize: String? = null,
) : PropertiesConfiguration()

/**
 * Read config file [configFileName] and return [PropertiesConfiguration] instance
 *
 * @param configFileName
 * @param type
 * @return corresponding configuration
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("TOO_LONG_FUNCTION")
fun readPropertiesFile(configFileName: String, type: PropertiesConfigurationType): PropertiesConfiguration? {
    try {
        val classLoader = AutomaticTestInitializator::class.java.classLoader
        val input = classLoader.getResource(configFileName)?.file
        input ?: run {
            log.error("Unable to find configuration file: $configFileName")
            return null
        }
        when (type) {
            PropertiesConfigurationType.WEB_CLIENT -> return Properties.decodeFromStringMap<WebClientProperties>(
                readProperties(input)
            )

            PropertiesConfigurationType.EVALUATED_TOOL -> return Properties.decodeFromStringMap<EvaluatedToolProperties>(
                readProperties(input)
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

/**
 * Read properties file as a map
 *
 * @param filePath a file to read
 * @return map of properties with values
 */
private fun readProperties(filePath: String): Map<String, String> = File(filePath).readLines()
    .filter { it.contains("=") }
    .associate { line ->
        line.split("=").map { it.trim() }.let {
            require(it.size == 2)
            it.first() to it.last()
        }
    }
