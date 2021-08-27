package org.cqfn.save.domain

import java.io.File
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

fun FileInfo.toFile(): File = File(name)

fun Path.toFileInfo() = FileInfo(name, getLastModifiedTime().toMillis(), fileSize())
