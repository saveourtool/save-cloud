/**
 * Utils for configuration of http client and evaluated tool
 */

package org.cqfn.save.apicli

import org.cqfn.save.domain.Jdk
import org.cqfn.save.domain.Python
import org.cqfn.save.domain.Sdk

import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import org.cqfn.save.`api-cli`.SaveCloudClient

private val log = LoggerFactory.getLogger(PropertiesUtils::class.java)


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
        val classLoader = SaveCloudClient::class.java.classLoader
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
