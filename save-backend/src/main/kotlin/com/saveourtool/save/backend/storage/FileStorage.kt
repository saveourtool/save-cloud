package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.ShortFileInfo
import com.saveourtool.save.storage.AbstractFileBasedStorage
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.extra.math.max
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name

/**
 * Storage for evaluated tools are loaded by users
 */
@Service
class FileStorage(
    configProperties: ConfigProperties,
) : AbstractFileBasedStorage.WithProjectCoordinates<FileKey>(Path.of(configProperties.fileStorage.location) / "storage") {
    /**
     * @param pathToContent
     * @return [Pair] of key and path to project path
     */
    override fun buildInnerKeyAndReturnProjectPath(pathToContent: Path): Pair<FileKey, Path> = Pair(
        FileKey(
            pathToContent.name,
            // assuming here, that we always store files in timestamp-based directories
            pathToContent.parent.name.toLong(),
        ),
        pathToContent.parent.parent
    )

    /**
     * @param projectPath
     * @param innerKey
     * @return path to content
     */
    override fun buildPathToContentFromProjectPath(projectPath: Path, innerKey: FileKey): Path =
            projectPath.resolve(innerKey.uploadedMillis.toString())
                .resolve(innerKey.name)

    /**
     * @param rootDir
     * @param pathToContent
     * @return true if there is 4 parts between pathToContent and rootDir
     */
    @Suppress("MAGIC_NUMBER", "MagicNumber")
    override fun isKey(rootDir: Path, pathToContent: Path): Boolean {
        val partsCount = generateSequence(pathToContent, Path::getParent)
            .takeWhile { it != rootDir }
            .count()
        return partsCount == 4  // organization + project + uploadedMills + fileName
    }

    /**
     * @param projectCoordinates
     * @param name name of evaluated tool
     * @return [FileKey] with highest [FileKey.uploadedMillis]
     */
    fun findLatestKeyByName(projectCoordinates: ProjectCoordinates, name: String): Mono<FileKey> = list(projectCoordinates)
        .filter { it.name == name }
        .max(Comparator.comparing { it.uploadedMillis })

    /**
     * @param projectCoordinates
     * @param shortFileInfoFlux
     * @return Flux of [FileInfo] found by [ShortFileInfo] with max uploadedMillis
     */
    fun convertToLatestFileInfo(projectCoordinates: ProjectCoordinates, shortFileInfoFlux: Flux<ShortFileInfo>): Flux<FileInfo> = shortFileInfoFlux
        .flatMap { shortFileInfo ->
            findLatestKeyByName(projectCoordinates, shortFileInfo.name)
                .flatMap { fileKey ->
                    contentSize(projectCoordinates, fileKey)
                        .map { sizeBytes ->
                            FileInfo(
                                fileKey.name,
                                fileKey.uploadedMillis,
                                sizeBytes,
                                shortFileInfo.isExecutable
                            )
                        }
                }
        }

    /**
     * @param projectCoordinates
     * @return a list of [FileInfo]'s
     */
    fun getFileInfoList(
        projectCoordinates: ProjectCoordinates,
    ): Flux<FileInfo> = list(projectCoordinates)
        .flatMap { fileKey ->
            contentSize(projectCoordinates, fileKey).map {
                FileInfo(
                    fileKey.name,
                    fileKey.uploadedMillis,
                    it,
                )
            }
        }

    /**
     * @param projectCoordinates
     * @param uploadedMillis
     * @return true if object removed, otherwise - false
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun deleteByUploadedMillis(
        projectCoordinates: ProjectCoordinates,
        uploadedMillis: Long,
    ): Mono<Boolean> = list(projectCoordinates)
        .filter { fileKey -> fileKey.uploadedMillis == uploadedMillis }
        .flatMap { delete(projectCoordinates, it) }
        .reduce(Boolean::and)
        .switchIfEmpty(Mono.just(false))

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
            part.filename(),
            uploadedMillis
        )
        upload(projectCoordinates, fileKey, part.content().map { it.asByteBuffer() })
            .map {
                FileInfo(
                    fileKey.name,
                    fileKey.uploadedMillis,
                    it
                )
            }
    }
}
