package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.countPartsTill
import com.saveourtool.save.utils.pathNamesTill
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Storage for evaluated tools are loaded by users
 */
@Service
class FileStorage(
    configProperties: ConfigProperties,
) : AbstractFileBasedStorage<FileKey>(
    Path.of(configProperties.fileStorage.location) / "storage",
    4,  // organization + project + uploadedMills + fileName
) {
    /**
     * @param projectCoordinates
     * @return list of keys in storage by [projectCoordinates]
     */
    fun list(projectCoordinates: ProjectCoordinates): Flux<FileKey> = list()
        .filter { it.projectCoordinates == projectCoordinates }

    @Suppress(
        "DestructuringDeclarationWithTooManyEntries"
    )
    override fun buildKey(rootDir: Path, pathToContent: Path): FileKey {
        val pathNames = pathToContent.pathNamesTill(rootDir)

        val (name, uploadedMillis, projectName, organizationName) = pathNames
        return FileKey(
            projectCoordinates = ProjectCoordinates(
                organizationName = organizationName,
                projectName = projectName,
            ),
            name = name,
            // assuming here, that we always store files in timestamp-based directories
            uploadedMillis = uploadedMillis.toLong(),
        )
    }

    override fun buildPathToContent(rootDir: Path, key: FileKey): Path = rootDir
        .resolve(key.projectCoordinates.organizationName)
        .resolve(key.projectCoordinates.projectName)
        .resolve(key.uploadedMillis.toString())
        .resolve(key.name)

    /**
     * @param projectCoordinates
     * @return a list of [FileInfo]'s
     */
    fun getFileInfoList(
        projectCoordinates: ProjectCoordinates,
    ): Flux<FileInfo> = list(projectCoordinates)
        .flatMap { fileKey ->
            contentSize(fileKey).map {
                FileInfo(
                    fileKey,
                    it,
                )
            }
        }

    /**
     * @param partMono file part
     * @param projectCoordinates
     * @return Mono with number of bytes saved
     * @throws FileAlreadyExistsException if file with this name already exists
     */
    fun uploadFilePart(
        partMono: Mono<FilePart>,
        projectCoordinates: ProjectCoordinates,
    ): Mono<FileInfo> = partMono.flatMap { part ->
        val uploadedMillis = System.currentTimeMillis()
        val fileKey = FileKey(
            projectCoordinates,
            part.filename(),
            uploadedMillis
        )
        upload(fileKey, part.content().map { it.asByteBuffer() })
            .map {
                FileInfo(
                    fileKey,
                    it
                )
            }
    }
}
