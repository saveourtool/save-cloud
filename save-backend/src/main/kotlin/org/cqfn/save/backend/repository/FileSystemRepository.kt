package org.cqfn.save.backend.repository

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.domain.FileInfo
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
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

/**
 * A repository which gives access to the files in a designated file system location
 */
@Repository
class FileSystemRepository(configProperties: ConfigProperties) {
    private val rootDir = Paths.get(configProperties.fileStorage.location).apply {
        if (!exists()) {
            createDirectories()
        }
    }

    /**
     * @return list of files in [rootDir]
     */
    fun getFilesList() = rootDir
        .listDirectoryEntries()

    /**
     * @param relativePath path to a file relative to [rootDir]
     * @return requested file as a [FileSystemResource]
     */
    fun getFile(relativePath: String): FileSystemResource =
            rootDir.resolve(relativePath).let(::FileSystemResource)

    /**
     * @param file a file to save
     */
    fun saveFile(file: Path) {
        file.copyTo(rootDir.resolve(file.name), overwrite = false)
    }

    /**
     * @param parts file parts
     * @return Mono with number of bytes saved
     */
    fun saveFile(parts: Mono<FilePart>): Mono<FileInfo> = parts.flatMap { part ->
        val uploadedMillis = System.currentTimeMillis()
        rootDir.resolve(part.filename()).run {
            if (notExists()) {
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
                    FileInfo(name, uploadedMillis, it)
                }
        }
    }
}
