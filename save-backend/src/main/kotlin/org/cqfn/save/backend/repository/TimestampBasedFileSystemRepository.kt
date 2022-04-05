package org.cqfn.save.backend.repository

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.domain.FileInfo
import org.cqfn.save.domain.ImageInfo

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.APPEND
import java.util.*
import java.util.stream.Collectors

import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

/**
 * A repository which gives access to the files in a designated file system location
 */
@Repository
class TimestampBasedFileSystemRepository(configProperties: ConfigProperties) {
    private val logger = LoggerFactory.getLogger(TimestampBasedFileSystemRepository::class.java)
    private val rootDir = (Paths.get(configProperties.fileStorage.location) / "storage").apply {
        if (!exists()) {
            createDirectories()
        }
    }
    private val rootDirImage = (Paths.get(configProperties.fileStorage.location) / "images" / "avatars").apply {
        if (!exists()) {
            createDirectories()
        }
    }

    private fun getStorageDir(fileInfo: FileInfo) = rootDir.resolve(fileInfo.uploadedMillis.toString())

    private fun createStorageDir(fileInfo: FileInfo) = getStorageDir(fileInfo).createDirectory()

    /**
     * @return list of files in [rootDir]
     */
    fun getFilesList() = rootDir.listDirectoryEntries()
        .filter { it.isDirectory() }
        .flatMap { it.listDirectoryEntries() }

    /**
     * @param fileInfo a FileInfo based on which a file should be located
     * @return requested file as a [FileSystemResource]
     */
    fun getFile(fileInfo: FileInfo): FileSystemResource = getPath(fileInfo).let(::FileSystemResource)

    /**
     * @param file a file to save
     * @return a FileInfo describing a saved file
     */
    fun saveFile(file: Path): FileInfo {
        val destination = rootDir
            .resolve(file.getLastModifiedTime().toMillis().toString())
            .createDirectories()
            .resolve(file.name)
        logger.info("Saving a new file into $destination")
        file.copyTo(destination, overwrite = false)
        return FileInfo(file.name, file.getLastModifiedTime().toMillis(), file.fileSize())
    }

    /**
     * @param parts file parts
     * @return Mono with number of bytes saved
     * @throws FileAlreadyExistsException if file with this name already exists
     */
    fun saveFile(parts: Mono<FilePart>): Mono<FileInfo> = parts.flatMap { part ->
        val uploadedMillis = System.currentTimeMillis()
        rootDir
            .resolve(uploadedMillis.toString())
            .createDirectories()
            .resolve(part.filename()).run {
                createFile(this, part)
                    .collect(Collectors.summingLong { it })
                    .map {
                        logger.info("Saved $it bytes into $this")
                        FileInfo(name, uploadedMillis, it)
                    }
            }
    }

    /**
     * @param imageName user or organization name
     * @param part file part
     * @param isOrganization flag of organization
     * @return Mono with number of bytes saved
     * @throws FileAlreadyExistsException if file with this name already exists
     */
    fun saveImage(part: Mono<FilePart>, imageName: String, isOrganization: Boolean = true): Mono<ImageInfo> = part.flatMap { part ->
        val uploadedDir = if (isOrganization) rootDirImage.resolve(imageName) else rootDirImage.resolve("users").resolve(imageName)

        uploadedDir.apply {
            if (exists()) {
                listDirectoryEntries().forEach { it.deleteIfExists() }
            }
        }.deleteIfExists()

        uploadedDir
            .createDirectories()
            .resolve(part.filename()).run {
                createFile(this, part)
                    .collect(Collectors.summingLong { it })
                    .map {
                        logger.info("Saved $it bytes into $this")
                        val relativePath = if (isOrganization) ("/$imageName/$name") else ("/users/$imageName/$name")
                        ImageInfo(relativePath)
                    }
            }
    }

    /**
     * @param path path to file
     * @param part file part
     * @return Flux<Long>
     */
    fun createFile(path: Path, part: FilePart): Flux<Long> {
        if (path.notExists()) {
            logger.info("Saving a new file from parts into $path")
            path.createFile()
        }
        return part.content().map { db ->
            path.outputStream(APPEND).use { os ->
                db.asInputStream().use {
                    it.copyTo(os)
                }
            }
        }
    }

    /**
     * Delete a file described by [fileInfo]
     *
     * @param fileInfo a [FileInfo] describing a file to be deleted
     * @return true if file has been deleted successfully, false otherwise
     */
    fun delete(fileInfo: FileInfo) = try {
        Files.walk(getStorageDir(fileInfo)).forEach {
            it.deleteExisting()
        }
        true
    } catch (fe: FileSystemException) {
        logger.error("Failed to delete file $fileInfo", fe)
        false
    }

    /**
     * @param fileInfo
     * @return path to the file in storage
     */
    fun getPath(fileInfo: FileInfo) = getStorageDir(fileInfo).resolve(fileInfo.name)
}
