/**
 * Utility methods to work with files using Okio
 */

@file:JvmName("FileUtilsJVM")

package com.saveourtool.save.agent.utils

import com.saveourtool.save.utils.fs
import okio.Path
import okio.Path.Companion.toPath
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.examples.Expander
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission

private val expander = Expander()

internal actual fun Path.extractZipTo(targetPath: Path) {
    logDebugCustom("Unzip $this into $targetPath")
    expander.expand(ArchiveStreamFactory.ZIP, toFile(), targetPath.toFile())
}

internal actual fun ByteArray.writeToFile(file: Path, mustCreate: Boolean) {
    fs.write(
        file = file,
        mustCreate = mustCreate,
    ) {
        write(this@writeToFile).flush()
    }
}

internal actual fun Path.markAsExecutable() {
    val file = this.toFile().toPath()
    Files.setPosixFilePermissions(file, Files.getPosixFilePermissions(file) +
            setOf(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE))
}
