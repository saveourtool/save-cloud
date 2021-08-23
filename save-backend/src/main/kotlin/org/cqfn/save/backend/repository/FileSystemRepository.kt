package org.cqfn.save.backend.repository

import org.cqfn.save.backend.configs.ConfigProperties
import org.springframework.core.io.FileSystemResource
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.APPEND
import java.util.stream.Collectors
import kotlin.io.path.copyTo
import kotlin.io.path.createFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists
import kotlin.io.path.outputStream

@Repository
class FileSystemRepository(configProperties: ConfigProperties) {
    private val rootDir = Paths.get(configProperties.fileStorage.location)

    fun getFilesList() = rootDir
        .listDirectoryEntries()

    fun getFile(relativePath: String): FileSystemResource =
            rootDir.resolve(relativePath).let(::FileSystemResource)

    fun saveFile(file: Path) {
        file.copyTo(rootDir, overwrite = false)
    }

    /**
     * @return Mono with number of bytes saved
     */
    fun saveFile(parts: Mono<FilePart>): Mono<Long> = parts.flatMap { part ->
        Paths.get(part.filename()).run {
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
        }
    }
}
