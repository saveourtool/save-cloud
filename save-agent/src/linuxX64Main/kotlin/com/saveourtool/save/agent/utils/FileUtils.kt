/**
 * Utility methods to work with files using Okio
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.utils.fs
import okio.Path
import platform.posix.S_IRGRP
import platform.posix.S_IROTH
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.S_IXUSR

internal actual fun Path.extractZipTo(targetPath: Path) {
    require(fs.metadata(targetPath).isDirectory)
    logDebugCustom("Unzip ${fs.canonicalize(this)} into ${fs.canonicalize(targetPath)}")
    platform.posix.system("unzip $this -d $targetPath")
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
    platform.posix.chmod(
        this.toString(),
        (S_IRUSR or S_IWUSR or S_IXUSR or S_IRGRP or S_IROTH).toUInt()
    )
}
