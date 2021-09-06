/**
 * Utils for `FileInfo` on JVM
 */

package org.cqfn.save.domain

import java.io.File
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

/**
 * @return a [File] with same name as `this`
 */
fun FileInfo.toFile(): File = File(name)

/**
 * @return [FileInfo] constructed from `this` [Path]
 */
fun Path.toFileInfo() = FileInfo(name, getLastModifiedTime().toMillis(), fileSize())
