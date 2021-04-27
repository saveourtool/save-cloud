@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.preprocessor.utils

import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest

private const val RADIX = 16

/**
 * @return hash of file content
 */
fun File.toHash(): String {
    val md = MessageDigest.getInstance("MD5")
    Files.newInputStream(Paths.get(this.path)).use { inputSrteam -> DigestInputStream(inputSrteam, md) }
    return BigInteger(1, md.digest()).toString(RADIX)
}
