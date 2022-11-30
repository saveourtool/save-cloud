package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.pathNamesTill
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.div

private const val TOOLS_PATH = "tools"

/**
 * Storage to keep all the tools on the disk
 */
@Component
class ToolStorage(
    configProperties: ConfigProperties,
) : AbstractFileBasedStorage<ToolKey>(Path.of(configProperties.fileStorage.location) / TOOLS_PATH) {
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    override fun buildKey(rootDir: Path, pathToContent: Path): ToolKey {
        val (_, ownerName, toolName, vcsTagName, executableName) = pathToContent.pathNamesTill(rootDir)
        return ToolKey(ownerName, toolName, vcsTagName, executableName)
    }

    override fun buildPathToContent(rootDir: Path, key: ToolKey): Path = rootDir / key.ownerName / key.toolName / key.vcsTagName / key.executableName
}
