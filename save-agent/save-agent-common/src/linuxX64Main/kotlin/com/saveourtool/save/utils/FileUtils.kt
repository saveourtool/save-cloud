/**
 * Native implementation for FileUtils
 */

package com.saveourtool.save.utils

import com.akuleshov7.ktoml.file.TomlFileReader
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import platform.posix.*

actual val fs: FileSystem = FileSystem.SYSTEM

@OptIn(UnsafeNumber::class)
actual fun Path.markAsExecutable() {
    val mode: mode_t = (S_IRUSR or S_IWUSR or S_IXUSR or S_IRGRP or S_IROTH).convert()
    chmod(this.toString(), mode)
}

actual fun ByteArray.writeToFile(file: Path, mustCreate: Boolean) {
    fs.write(file = file, mustCreate = mustCreate) { write(this@writeToFile).flush() }
}

actual inline fun <reified C : Any> parseConfig(configPath: Path): C = TomlFileReader.decodeFromFile(
    serializer(),
    configPath.toString(),
)
