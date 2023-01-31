/**
 * Utils to parse save-demo-agent configuration
 */

package com.saveourtool.save.demo.agent.utils

import com.saveourtool.save.demo.agent.DemoAgentConfig

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

internal val fs = FileSystem.SYSTEM

/**
 * Parse config file
 *
 * @param configName name of a config file, agent.properties by default
 * @return [DemoAgentConfig] filled with configuration information
 */
@OptIn(ExperimentalSerializationApi::class)
fun parseConfig(configName: String = "agent.properties"): DemoAgentConfig {
    val propertiesFile = configName.toPath()
    require(fs.exists(propertiesFile)) { "Could not find $configName file." }
    return Properties.decodeFromStringMap(readProperties(propertiesFile.name))
}

private fun readFile(filePath: String): List<String> = try {
    val path = filePath.toPath()
    fs.read(path) { generateSequence { readUtf8Line() }.toList() }
} catch (e: FileNotFoundException) {
    emptyList()
}

private fun readProperties(filePath: String): Map<String, String> = readFile(filePath)
    .associate { line ->
        line.split("=").map { it.trim() }.let {
            require(it.size == 2)
            it.first() to it.last()
        }
    }
