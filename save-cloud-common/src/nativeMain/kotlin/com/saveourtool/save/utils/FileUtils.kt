/**
 * Native implementation for FileUtils
 */

package com.saveourtool.save.utils

import com.akuleshov7.ktoml.file.TomlFileReader
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.*

import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.serialization.serializer

actual val fs: FileSystem = FileSystem.SYSTEM

@OptIn(UnsafeNumber::class)
actual fun Path.markAsExecutable() {
    val mode: mode_t = (S_IRUSR or S_IWUSR or S_IXUSR or S_IRGRP or S_IROTH).convert()
    chmod(this.toString(), mode)
}

actual fun ByteArray.writeToFile(file: Path, mustCreate: Boolean) {
    fs.write(file = file, mustCreate = mustCreate) { write(this@writeToFile).flush() }
}

/**
 * Write [lines] to file with name [fileName]
 *
 * @param fileName name of a file
 * @param lines lines to be written to file with name [fileName]
 * @return path to file
 */
fun FileSystem.createAndWrite(fileName: String, lines: List<String>) = fileName.toPath().also { path ->
    write(path, true) { lines.forEach { codeLine -> writeUtf8("$codeLine\n") } }
}

/**
 * Write [lines] to file with name [fileName] if needed
 *
 * @param fileName name of a file
 * @param lines lines to be written to file with name [fileName]
 * @return path to file if both [fileName] and [lines] are provided, null otherwise
 */
fun FileSystem.createAndWriteIfNeeded(fileName: String?, lines: List<String>?) = fileName?.toPath()?.also { path ->
    write(path, true) { lines?.forEach { codeLine -> writeUtf8("$codeLine\n") } }
}

/**
 * Mark all files in [dir] as executable.
 *
 * Note that files from subdirectories are not marked as executable.
 *
 * @param dir [Path] to directory
 */
fun markAllFilesAsExecutable(dir: Path) {
    fs.list(dir).filter { fs.metadata(it).isRegularFile }.map { it.markAsExecutable() }
}

actual inline fun <reified C : Any> parseConfig(configPath: Path): C = TomlFileReader.decodeFromFile(
    serializer(),
    configPath.toString(),
)
