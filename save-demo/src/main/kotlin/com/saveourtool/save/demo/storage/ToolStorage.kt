package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name

private const val TOOLS_PATH = "tools"

/**
 * Storage to keep all the tools on the disk
 */
@Component
class ToolStorage(
    configProperties: ConfigProperties,
) : AbstractFileBasedStorage<ToolKey>(Path.of(configProperties.fileStorage.location) / TOOLS_PATH) {
    @Suppress("COMPLEX_EXPRESSION")
    override fun buildKey(rootDir: Path, pathToContent: Path): ToolKey = ToolKey(
        pathToContent.parent.parent.parent.name,
        pathToContent.parent.parent.name,
        pathToContent.parent.name,
        pathToContent.name,
    )

    override fun buildPathToContent(rootDir: Path, key: ToolKey): Path = rootDir / key.ownerName / key.toolName / key.vcsTagName / key.executableName
}
