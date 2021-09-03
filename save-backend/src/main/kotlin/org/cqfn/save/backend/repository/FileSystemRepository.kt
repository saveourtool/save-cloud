package org.cqfn.save.backend.repository

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.domain.FileInfo

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.APPEND
import java.util.stream.Collectors

import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
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
class FileSystemRepository(configProperties: ConfigProperties) {
    private val logger = LoggerFactory.getLogger(FileSystemRepository::class.java)
    private val rootDir = Paths.get(configProperties.fileStorage.location).apply {
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
    fun getFile(fileInfo: FileInfo): FileSystemResource = getStorageDir(fileInfo)
        .resolve(fileInfo.name)
        .let(::FileSystemResource)

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
                if (notExists()) {
                    logger.info("Saving a new file from parts into $this")
                    createFile()
                }
                part.content().map { db ->
                    outputStream(APPEND).use { os ->
                        db.asInputStream().use {
                            it.copyTo(os)
                        }
                    }
                }
                    .collect(Collectors.summingLong { it })
                    .map {
                        logger.info("Saved $it bytes into $this")
                        FileInfo(name, uploadedMillis, it)
                    }
            }
    }
}
