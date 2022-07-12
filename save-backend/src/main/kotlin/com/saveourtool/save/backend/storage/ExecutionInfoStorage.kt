package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name

/**
 * A storage for storing additional data (ExecutionInfo) associated with test results
 */
@Service
class ExecutionInfoStorage(
    configProperties: ConfigProperties,
) : AbstractFileBasedStorage<Long>(Path.of(configProperties.fileStorage.location) / "debugInfo") {
    /**
     * @param pathToContent
     * @return true if filename is [FILE_NAME]
     */
    override fun isKey(pathToContent: Path): Boolean = pathToContent.name == FILE_NAME

    /**
     * @param rootDir
     * @param pathToContent
     * @return executionId from parent name
     */
    override fun buildKey(rootDir: Path, pathToContent: Path): Long = pathToContent.parent.name.toLong()

    /**
     * @param rootDir
     * @param key
     * @return [Path] to content
     */
    override fun buildPathToContent(rootDir: Path, key: Long): Path = rootDir / key.toString() / FILE_NAME

    companion object {
        private const val FILE_NAME = "execution-info.json"
    }
}
