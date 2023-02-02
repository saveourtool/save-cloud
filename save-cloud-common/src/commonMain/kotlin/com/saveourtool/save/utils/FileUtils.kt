/**
 * FileUtils expected declarations
 */

package com.saveourtool.save.utils

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

expect val fs: FileSystem

/**
 * Parse config file
 * Notice that [C] should be serializable
 *
 * @param configPath path to toml config file
 * @return [C] filled with configuration information
 */
expect inline fun <reified C : Any> parseConfig(configPath: Path): C

/**
 * Parse config file
 * Notice that [C] should be serializable
 *
 * @param configName name of a toml config file, agent.toml by default
 * @return [C] filled with configuration information
 */
inline fun <reified C : Any> parseConfig(configName: String = "agent.toml"): C = parseConfig(configName.toPath())
