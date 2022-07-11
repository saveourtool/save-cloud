package com.saveourtool.save.backend.repository

import com.saveourtool.save.backend.storage.AvatarKey
import com.saveourtool.save.backend.storage.AvatarStorage
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.domain.*
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.toDataBufferFlux

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.ByteBuffer

import java.nio.file.Path
import kotlin.io.path.*

/**
 * A repository which gives access to the files in a designated file system location
 */
@Repository
class TimestampBasedFileSystemRepository(
    private val fileStorage: FileStorage,
    private val avatarStorage: AvatarStorage,
) {
    private val logger = LoggerFactory.getLogger(TimestampBasedFileSystemRepository::class.java)

    /**
     * @param projectCoordinates
     * @return a list of FileInfo's
     */
    fun getFileInfoList(
        projectCoordinates: ProjectCoordinates,
    ): List<FileInfo> = fileStorage.list(projectCoordinates)
        .flatMap { fileKey ->
            fileStorage.contentSize(projectCoordinates, fileKey).map {
                FileInfo(
                    fileKey.name,
                    fileKey.uploadedMillis,
                    it,
                )
            }
        }.collectList()
        .subscribeOn(Schedulers.immediate())
        .toFuture()
        .get()

    /**
     * @param projectCoordinates
     * @param creationTimestamp
     * @return a list of FileInfo's
     */
    fun deleteFileByDirName(
        projectCoordinates: ProjectCoordinates,
        creationTimestamp: String,
    ): Boolean = fileStorage.list(projectCoordinates)
        .filter { fileKey -> fileKey.uploadedMillis == creationTimestamp.toLong() }
        .flatMap { fileStorage.delete(projectCoordinates, it) }
        .collectList()
        .subscribeOn(Schedulers.immediate())
        .toFuture()
        .get()
        .singleOrNull()
        ?: false

    /**
     * @param shortFileInfo
     * @param projectCoordinates
     * @return FileInfo, obtained from [shortFileInfo]
     */
    fun getFileInfoByShortInfo(
        shortFileInfo: ShortFileInfo,
        projectCoordinates: ProjectCoordinates,
    ): FileInfo = fileStorage.list(projectCoordinates)
        .filter { fileKey -> fileKey.name == shortFileInfo.name }
        .flatMap { fileKey ->
            fileStorage.contentSize(projectCoordinates, fileKey).map {
                FileInfo(
                    name = fileKey.name,
                    uploadedMillis = fileKey.uploadedMillis,
                    sizeBytes = it,
                    isExecutable = shortFileInfo.isExecutable
                )
            }
        }.collectList()
        .subscribeOn(Schedulers.immediate())
        .toFuture()
        .get()
        .single()

    /**
     * @param fileInfo a FileInfo based on which a file should be located
     * @param projectCoordinates
     * @return requested file as a [FileSystemResource]
     */
    fun getFileContent(
        fileInfo: FileInfo,
        projectCoordinates: ProjectCoordinates
    ): Flux<ByteBuffer> = fileStorage.download(projectCoordinates, fileInfo.toFileKey())

    /**
     * @param file a file to save
     * @param projectCoordinates
     * @return a FileInfo describing a saved file
     */
    fun saveFile(
        file: Path,
        projectCoordinates: ProjectCoordinates,
    ): FileInfo {
        val fileInfo = FileInfo(file.name, file.getLastModifiedTime().toMillis(), file.fileSize())
        val fileKey = FileKey(fileInfo)
        fileStorage.upload(projectCoordinates, fileKey, file.toDataBufferFlux().map { it.asByteBuffer() })
            .subscribeOn(Schedulers.immediate())
            .toFuture()
            .get()
        return fileInfo
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
        val fileKey = FileKey(
            part.filename(),
            uploadedMillis
        )
        fileStorage.upload(projectCoordinates, fileKey, part.content().map { it.asByteBuffer() })
            .map {
                FileInfo(
                    fileKey.name,
                    fileKey.uploadedMillis,
                    it
                )
            }
    }

    /**
     * @param imageName user or organization name
     * @param partMono file part
     * @param type type of avatar
     * @return Mono with number of bytes saved
     * @throws FileAlreadyExistsException if file with this name already exists
     */
    fun saveImage(partMono: Mono<FilePart>, imageName: String, type: AvatarType = AvatarType.ORGANIZATION): Mono<ImageInfo> = partMono.flatMap { part ->
        val avatarKey = AvatarKey(
            type,
            imageName,
            part.filename()
        )
        avatarStorage.upload(avatarKey, part.content().map { it.asByteBuffer() }).map {
            logger.info("Saved $it bytes of $avatarKey")
            ImageInfo(avatarKey.getRelativePath())
        }
    }

    /**
     * @param fileInfo
     * @param projectCoordinates
     * @return path to the file in storage
     */
    fun getPath(
        fileInfo: FileInfo,
        projectCoordinates: ProjectCoordinates,
    ): Path = fileStorage.getPath(projectCoordinates, fileInfo.toFileKey())
}
