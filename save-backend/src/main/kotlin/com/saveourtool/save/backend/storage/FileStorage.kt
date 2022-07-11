package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.storage.AbstractFileBasedStorage
import org.springframework.stereotype.Service
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
     * @param projectCoordinates
     * @param name name of evaluated tool
     * @return [FileKey] with highest [FileKey.uploadedMillis]
     */
    fun findLatestKeyByName(projectCoordinates: ProjectCoordinates, name: String): Mono<FileKey> = list(projectCoordinates)
        .filter { it.name == name }
        .max(Comparator.comparing { it.uploadedMillis })
}
