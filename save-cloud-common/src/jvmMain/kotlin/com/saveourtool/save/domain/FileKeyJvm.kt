/**
 * Utils for `FileKey` on JVM
 */

package com.saveourtool.save.domain

import java.nio.file.Path
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

/**
 * @param projectCoordinates
 * @return [FileKey] constructed from `this` [Path]
 */
fun Path.toFileKey(projectCoordinates: ProjectCoordinates) = FileKey(projectCoordinates, name, getLastModifiedTime().toMillis())
