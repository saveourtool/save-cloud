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
import kotlinx.cinterop.pointed
import kotlinx.serialization.serializer

actual val fs: FileSystem = FileSystem.SYSTEM

@OptIn(UnsafeNumber::class)
@Suppress("TooGenericExceptionThrown")
actual fun Path.permitReadingOnlyForOwner(ownerName: String, groupName: String) {
    val owner = requireNotNull(getpwnam(ownerName)) { "Could not find user with name $ownerName" }
    val group = requireNotNull(getgrnam(groupName)) { "Could not find group with name $groupName" }

    if (chown(toString(), owner.pointed.pw_uid, group.pointed.gr_gid) != 0) {
        throw RuntimeException("Could not change file owner or group")
    }

    val mode: mode_t = S_IRUSR.convert()
    if (chmod(toString(), mode) != 0) {
        throw RuntimeException("Could not change file permissions")
    }
}

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

actual inline fun <reified C : Any> parseConfig(configPath: Path): C = TomlFileReader.decodeFromFile(
    serializer(),
    configPath.toString(),
)
