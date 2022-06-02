package com.saveourtool.save.backend.repository

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.ImageInfo
import com.saveourtool.save.domain.ShortFileInfo
import com.saveourtool.save.utils.AvatarType

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
import java.util.stream.Collectors
import kotlin.io.path.*

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

    private fun getStorageDir(
        fileInfo: FileInfo,
        organizationName: String,
        projectName: String,
    ) = rootDir
        .resolve(organizationName)
        .resolve(projectName)
        .resolve(fileInfo.uploadedMillis.toString())

    private fun createStorageDir(
        fileInfo: FileInfo,
        organizationName: String,
        projectName: String,
    ) = getStorageDir(fileInfo, organizationName, projectName).createDirectory()

    /**
     * @param organizationName
     * @param projectName
     * @return list of files in [rootDir]
     */
    fun getFilesList(
        organizationName: String,
        projectName: String,
    ): List<Path> {
        rootDir
            .resolve(organizationName)
            .resolve(projectName).let { path ->
                if (path.exists()) {
                    return path.listDirectoryEntries()
                        .filter { it.isDirectory() }
                        .flatMap { it.listDirectoryEntries() }
                }
            }
        return emptyList()
    }

    /**
     * @param organizationName
     * @param projectName
     * @return a list of FileInfo's
     */
    fun getFileInfoList(
        organizationName: String,
        projectName: String,
    ) = getFilesList(organizationName, projectName).map {
        FileInfo(
            it.name,
            // assuming here, that we always store files in timestamp-based directories
            it.parent.name.toLong(),
            it.fileSize(),
        )
    }

    /**
     * @param shortFileInfo
     * @param organizationName
     * @param projectName
     * @return FileInfo, obtained from [shortFileInfo]
     */
    fun getFileInfoByShortInfo(
        shortFileInfo: ShortFileInfo,
        organizationName: String,
        projectName: String,
    ) = getFileInfoList(organizationName, projectName).first { it.name == shortFileInfo.name }.copy(isExecutable = shortFileInfo.isExecutable)

    /**
     * @param fileInfo a FileInfo based on which a file should be located
     * @param organizationName
     * @param projectName
     * @return requested file as a [FileSystemResource]
     */
    fun getFile(
        fileInfo: FileInfo,
        organizationName: String,
        projectName: String,
    ): FileSystemResource = getPath(fileInfo, organizationName, projectName).let(::FileSystemResource)

    /**
     * @param file a file to save
     * @param organizationName
     * @param projectName
     * @return a FileInfo describing a saved file
     */
    fun saveFile(
        file: Path,
        organizationName: String,
        projectName: String,
    ): FileInfo {
        val destination = rootDir
            .resolve(organizationName)
            .resolve(projectName)
            .resolve(file.getLastModifiedTime().toMillis().toString())
            .createDirectories()
            .resolve(file.name)
        logger.info("Saving a new file into $destination")
        file.copyTo(destination, overwrite = false)
        return FileInfo(file.name, file.getLastModifiedTime().toMillis(), file.fileSize())
    }

    /**
     * @param parts file parts
     * @param organizationName
     * @param projectName
     * @return Mono with number of bytes saved
     * @throws FileAlreadyExistsException if file with this name already exists
     */
    fun saveFile(
        parts: Mono<FilePart>,
        organizationName: String,
        projectName: String,
    ): Mono<FileInfo> = parts.flatMap { part ->
        val uploadedMillis = System.currentTimeMillis()
        rootDir
            .resolve(organizationName)
            .resolve(projectName)
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
     * @param type type of avatar
     * @return Mono with number of bytes saved
     * @throws FileAlreadyExistsException if file with this name already exists
     */
    fun saveImage(part: Mono<FilePart>, imageName: String, type: AvatarType = AvatarType.ORGANIZATION): Mono<ImageInfo> = part.flatMap { part ->
        val uploadedDir = rootDirImage.resolve(getRelativePath(type, imageName))

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
                        val relativePath = "/${getRelativePath(type, imageName)}/$name"
                        ImageInfo(relativePath)
                    }
            }
    }

    private fun getRelativePath(type: AvatarType, imageName: String): String =
            when (type) {
                AvatarType.ORGANIZATION -> imageName
                AvatarType.USER -> "users/$imageName"
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
     * @param organizationName
     * @param projectName
     * @return true if file has been deleted successfully, false otherwise
     */
    fun delete(
        fileInfo: FileInfo,
        organizationName: String,
        projectName: String,
    ) = try {
        Files.walk(getStorageDir(fileInfo, organizationName, projectName)).forEach {
            it.deleteExisting()
        }
        true
    } catch (fe: FileSystemException) {
        logger.error("Failed to delete file $fileInfo", fe)
        false
    }

    /**
     * @param fileInfo
     * @param organizationName
     * @param projectName
     * @return path to the file in storage
     */
    fun getPath(
        fileInfo: FileInfo,
        organizationName: String,
        projectName: String,
    ) = getStorageDir(fileInfo, organizationName, projectName).resolve(fileInfo.name)
}
