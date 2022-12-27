package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.FileService
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.pathNamesTill
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Storage for evaluated tools are loaded by users
 */
@Service
class FileStorage(
    configProperties: ConfigProperties,
    private val fileService: FileService,
) : AbstractFileBasedStorage<FileDto>(Path.of(configProperties.fileStorage.location) / "storage", PATH_PARTS_COUNT) {
    override fun buildKey(rootDir: Path, pathToContent: Path): FileDto {
        val pathNames = pathToContent.pathNamesTill(rootDir)
        val (fileId) = pathNames
        return fileService.get(fileId.toLong()).toDto()
    }

    override fun buildPathToContent(rootDir: Path, key: FileDto): Path = rootDir
        .resolve(key.requiredId().toString())

    companion object {
        private const val PATH_PARTS_COUNT = 1  // fileId
    }
}
