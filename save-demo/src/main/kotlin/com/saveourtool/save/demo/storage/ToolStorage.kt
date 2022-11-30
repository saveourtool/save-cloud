package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.toByteBufferFlux
import com.saveourtool.save.utils.upload
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.nio.file.Path
import javax.annotation.PostConstruct
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
    /**
     * Todo: Implement DownloadToolService
     */
    @PostConstruct
    fun downloadTools() {
        val supportedTools = listOf(
            ToolKey("diktat", "1.2.3", "diktat"),
            ToolKey("diktat", "1.2.3", "diktat.cmd"),
            ToolKey("ktlint", "0.47.1", "ktlint-cli"),
            ToolKey("ktlint", "0.47.1", "ktlint-cli.cmd"),
        )
        list().collectList()
            .flatMapIterable { availableFiles ->
                supportedTools.filter {
                    it !in availableFiles
                }
            }
            .flatMap { key ->
                upload(key, ClassPathResource(key.executableName).toByteBufferFlux()).thenReturn(key)
            }
            .subscribe()
    }

    override fun buildKey(rootDir: Path, pathToContent: Path): ToolKey = ToolKey(
        pathToContent.parent.parent.name,
        pathToContent.parent.name,
        pathToContent.name
    )

    override fun buildPathToContent(rootDir: Path, key: ToolKey): Path = rootDir / key.toolName / key.version / key.executableName
}
