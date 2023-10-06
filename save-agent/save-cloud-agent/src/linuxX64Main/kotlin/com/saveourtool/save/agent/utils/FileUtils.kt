@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

@file:JvmName("FileUtilsJVM")

package com.saveourtool.save.agent.utils

import com.akuleshov7.ktoml.file.TomlFileReader
import okio.FileSystem

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.*

import kotlinx.serialization.serializer

actual val fs: FileSystem = FileSystem.SYSTEM

actual fun okio.Path.markAsExecutable() {
    val file = this.toFile().toPath()
    Files.setPosixFilePermissions(file, Files.getPosixFilePermissions(file) + EnumSet.of(
        PosixFilePermission.OWNER_EXECUTE,
        PosixFilePermission.GROUP_EXECUTE,
        PosixFilePermission.OTHERS_EXECUTE,
    ))
}

actual fun ByteArray.writeToFile(file: okio.Path, mustCreate: Boolean) {
    fs.write(
        file = file,
        mustCreate = mustCreate,
    ) {
        write(this@writeToFile).flush()
    }
}

actual inline fun <reified C : Any> parseConfig(configPath: okio.Path): C = TomlFileReader.decodeFromFile(
    serializer(),
    configPath.toString(),
)
