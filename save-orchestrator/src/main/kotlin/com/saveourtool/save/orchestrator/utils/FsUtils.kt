/**
 * Methods to work with file system
 */

package com.saveourtool.save.orchestrator.utils

import com.saveourtool.save.utils.warn
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.name

internal val allExecute = setOf(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE)

/**
 * Try to add executable permissions for file denoted by [this]
 */
context(LoggingContext)
internal fun Path.tryMarkAsExecutable() {
    try {
        Files.setPosixFilePermissions(this, Files.getPosixFilePermissions(this) + allExecute)
    } catch (ex: UnsupportedOperationException) {
        logger.warn(ex) { "Failed to mark file ${this.name} as executable" }
    }
}

/**
 * Change owner of all files under [directory] to user named [user]
 */
internal fun changeOwnerRecursively(directory: Path, user: String) {
    val lookupService = directory.fileSystem.userPrincipalLookupService
    directory.toFile().walk().forEach { file ->
        Files.getFileAttributeView(file.toPath(), PosixFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS).apply {
            setGroup(lookupService.lookupPrincipalByGroupName(user))
            setOwner(lookupService.lookupPrincipalByName(user))
        }
    }
}
