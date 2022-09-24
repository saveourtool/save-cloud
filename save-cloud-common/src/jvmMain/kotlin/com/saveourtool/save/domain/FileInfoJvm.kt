/**
 * Utils for `FileInfo` on JVM
 */

package com.saveourtool.save.domain

import java.io.File
import java.nio.file.Path
import kotlin.io.path.fileSize

/**
 * @return a [File] with same name as `this`
 */
fun FileInfo.toFile(): File = File(key.name)

/**
 * @param projectCoordinates
 * @return [FileInfo] constructed from `this` [Path]
 */
fun Path.toFileInfo(projectCoordinates: ProjectCoordinates) = FileInfo(toFileKey(projectCoordinates), fileSize())
