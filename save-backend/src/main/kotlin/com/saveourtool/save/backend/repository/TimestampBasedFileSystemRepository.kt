package com.saveourtool.save.backend.repository

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.ImageInfo
import com.saveourtool.save.domain.ProjectCoordinates
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

    private fun getFileResourcesDir(
        projectCoordinates: ProjectCoordinates,
    ) = rootDir
        .resolve(projectCoordinates.organizationName)
        .resolve(projectCoordinates.projectName)

    private fun getStorageDir(
        fileInfo: FileInfo,
        projectCoordinates: ProjectCoordinates,
    ) = getFileResourcesDir(projectCoordinates)
        .resolve(fileInfo.uploadedMillis.toString())

    private fun createStorageDir(
        fileInfo: FileInfo,
        projectCoordinates: ProjectCoordinates,
    ) = getStorageDir(fileInfo, projectCoordinates).createDirectory()

    /**
     * @param projectCoordinates
     * @return list of files in [rootDir]
     */
    fun getFilesList(
        projectCoordinates: ProjectCoordinates,
    ): List<Path> =
            getFileResourcesDir(projectCoordinates)
                .takeIf { it.exists() }
                ?.listDirectoryEntries()
                ?.filter { it.isDirectory() }
                ?.flatMap { it.listDirectoryEntries() }
                ?: emptyList()

    /**
     * @param projectCoordinates
     * @return a list of FileInfo's
     */
    fun getFileInfoList(
        projectCoordinates: ProjectCoordinates,
    ) = getFilesList(projectCoordinates).map {
        FileInfo(
            it.name,
            // assuming here, that we always store files in timestamp-based directories
            it.parent.name.toLong(),
            it.fileSize(),
        )
    }

    /**
     * @param shortFileInfo
     * @param projectCoordinates
     * @return FileInfo, obtained from [shortFileInfo]
     */
    fun getFileInfoByShortInfo(
        shortFileInfo: ShortFileInfo,
        projectCoordinates: ProjectCoordinates,
    ) = getFileInfoList(projectCoordinates).first { it.name == shortFileInfo.name }.copy(isExecutable = shortFileInfo.isExecutable)

    /**
     * @param fileInfo a FileInfo based on which a file should be located
     * @param projectCoordinates
     * @return requested file as a [FileSystemResource]
     */
    fun getFile(
        fileInfo: FileInfo,
        projectCoordinates: ProjectCoordinates
    ): FileSystemResource = getPath(fileInfo, projectCoordinates).let(::FileSystemResource)

    /**
     * @param file a file to save
     * @param projectCoordinates
     * @return a FileInfo describing a saved file
     */
    fun saveFile(
        file: Path,
        projectCoordinates: ProjectCoordinates,
    ): FileInfo {
        val destination = getFileResourcesDir(projectCoordinates)
            .resolve(file.getLastModifiedTime().toMillis().toString())
            .createDirectories()
            .resolve(file.name)
        logger.info("Saving a new file into $destination")
        file.copyTo(destination, overwrite = false)
        return FileInfo(file.name, file.getLastModifiedTime().toMillis(), file.fileSize())
    }

    /**
     * @param parts file parts
     * @param projectCoordinates
     * @return Mono with number of bytes saved
     * @throws FileAlreadyExistsException if file with this name already exists
     */
    fun saveFile(
        parts: Mono<FilePart>,
        projectCoordinates: ProjectCoordinates,
    ): Mono<FileInfo> = parts.flatMap { part ->
        val uploadedMillis = System.currentTimeMillis()
        getFileResourcesDir(projectCoordinates)
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
     * @param projectCoordinates
     * @return true if file has been deleted successfully, false otherwise
     */
    fun delete(
        fileInfo: FileInfo,
        projectCoordinates: ProjectCoordinates,
    ) = try {
        Files.walk(getStorageDir(fileInfo, projectCoordinates)).forEach {
            it.deleteExisting()
        }
        true
    } catch (fe: FileSystemException) {
        logger.error("Failed to delete file $fileInfo", fe)
        false
    }

    /**
     * @param fileInfo
     * @param projectCoordinates
     * @return path to the file in storage
     */
    fun getPath(
        fileInfo: FileInfo,
        projectCoordinates: ProjectCoordinates,
    ) = getStorageDir(fileInfo, projectCoordinates).resolve(fileInfo.name)
}
