@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.preprocessor.utils

import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Properties

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.decodeFromMap

private const val RADIX = 16

/**
 * @return hash of file content
 */
fun File.toHash(): String {
    val md = MessageDigest.getInstance("MD5")
    Files.newInputStream(Paths.get(this.path)).use { inputStream -> DigestInputStream(inputStream, md) }
    return BigInteger(1, md.digest()).toString(RADIX)
}

/**
 * Decodes an instance of [T] from properties file
 *
 * @param file a properties file
 * @return a deserialized instance of [T]
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
inline fun <reified T> decodeFromPropertiesFile(file: File): T {
    val rawProperties = Properties().apply {
        load(file.inputStream())
    }
    return kotlinx.serialization.properties.Properties.decodeFromMap(
        rawProperties as Map<String, Any>
    )
}
