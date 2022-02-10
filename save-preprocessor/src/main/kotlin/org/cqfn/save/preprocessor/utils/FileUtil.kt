@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.preprocessor.utils

import org.slf4j.LoggerFactory
import org.springframework.util.FileSystemUtils

import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Properties

import kotlin.io.path.createDirectories
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.properties.decodeFromMap

private const val RADIX = 16
private val log = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)

/**
 * @return hash of file content
 */
fun File.toHash(): String {
    val md = MessageDigest.getInstance("MD5")
    Files.newInputStream(Paths.get(this.path)).use { inputStream ->
        DigestInputStream(inputStream, md).readAllBytes()
    }
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

/**
 * Create a temporary directory with name based on [seeds]
 *
 * @param seeds a list of strings for directory name creation
 * @param repository name of the repository used for the creation of tmp dir
 * @param deleteExisting whether to delete existing directory
 * @return a [File] representing the created temporary directory
 */
internal fun generateDirectory(seeds: List<String>, repository: String, deleteExisting: Boolean = true): File {
    val tmpDir = getTmpDirName(seeds, repository)
    log.info("For $seeds: starting generate directory $tmpDir")
    if (tmpDir.exists() && deleteExisting) {
        deleteDirectory(tmpDir)
    } else if (tmpDir.exists() && !deleteExisting) {
        log.info("Directory $tmpDir already exists and delete strategy wasn't provided, won't perform any actions")
        return tmpDir
    }
    return generateDirectory(tmpDir)
}

/**
 * @param tmpDir directory which need to be deleted
 */
internal fun deleteDirectory(tmpDir: File) {
    try {
        if (FileSystemUtils.deleteRecursively(tmpDir.toPath())) {
            log.debug("Dir $tmpDir was deleted")
        }
    } catch (e: IOException) {
        log.error("Couldn't properly delete $tmpDir", e)
    }
}

/**
 * @param tmpDir directory which need to be created
 * @return a [File] representing the created temporary directory
 */
@Suppress("TooGenericExceptionCaught")
internal fun generateDirectory(tmpDir: File): File {
    try {
        tmpDir.toPath().createDirectories()
        log.debug("Dir $tmpDir was created")
    } catch (e: Exception) {
        log.error("Couldn't create directories for $tmpDir", e)
    }
    return tmpDir
}

/**
 * @param seeds
 * @param repository
 * @return the file entity with a hashcode created from the list of seeds
 */
internal fun getTmpDirName(seeds: List<String>, repository: String) = File("$repository/${seeds.hashCode()}")
