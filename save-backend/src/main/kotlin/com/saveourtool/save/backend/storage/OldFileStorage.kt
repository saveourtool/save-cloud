package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.nio.file.Path
import javax.annotation.PostConstruct
import kotlin.io.path.div

/**
 * Storage for evaluated tools are loaded by users
 */
@Service
class OldFileStorage(
    configProperties: ConfigProperties,
) : AbstractFileBasedStorage<FileKey>(Path.of(configProperties.fileStorage.location) / "storage", PATH_PARTS_COUNT) {
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

    companion object {
        private const val PATH_PARTS_COUNT = 4  // organization + project + uploadedMills + fileName
    }
}
