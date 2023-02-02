/**
 * Native implementation for FileUtils
 */

package com.saveourtool.save.utils

import com.akuleshov7.ktoml.file.TomlFileReader
import okio.FileSystem
import okio.Path

import kotlinx.serialization.serializer

actual val fs: FileSystem = FileSystem.SYSTEM

actual inline fun <reified C : Any> parseConfig(configPath: Path): C {
    require(fs.exists(configPath)) { "Could not find $configPath file." }
    return TomlFileReader.decodeFromFile(serializer(), configPath.toString())
}
