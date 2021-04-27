package org.cqfn.save.preprocessor.utils

import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest

private const val RADIX = 16

fun File.toHash(): String {
    val md = MessageDigest.getInstance("MD5")
    Files.newInputStream(Paths.get(this.path)).use { inputSrteam -> DigestInputStream(inputSrteam, md) }
    return BigInteger(1, md.digest()).toString(RADIX)
}
