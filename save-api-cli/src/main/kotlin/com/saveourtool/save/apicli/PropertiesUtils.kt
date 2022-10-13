/**
 * Utils for configuration of http client and evaluated tool
 */

package com.saveourtool.save.apicli

import com.saveourtool.save.api.SaveCloudClient
import com.saveourtool.save.api.config.EvaluatedToolProperties
import com.saveourtool.save.api.config.PropertiesConfiguration
import com.saveourtool.save.api.config.PropertiesConfigurationType
import com.saveourtool.save.api.config.WebClientProperties

import org.slf4j.LoggerFactory

import java.io.File
import java.io.IOException

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

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
