package org.cqfn.save.preprocessor.utils

import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

fun File.toHash(): String {
    val md = MessageDigest.getInstance("SHA-256")
    return BigInteger(1, md.digest(this.readBytes())).toString(16)
}