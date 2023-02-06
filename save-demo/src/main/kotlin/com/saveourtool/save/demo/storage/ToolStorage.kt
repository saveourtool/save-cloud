package com.saveourtool.save.demo.storage

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.*
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.*

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
        val (executableName, vcsTagName, toolName, ownerName) = pathToContent.pathNamesTill(rootDir)
        return ToolKey(ownerName, toolName, vcsTagName, executableName)
    }

    override fun buildPathToContent(rootDir: Path, key: ToolKey): Path = rootDir / key.organizationName / key.projectName / key.version / key.fileName

    private fun downloadToTempDir(
        tempDir: Path,
        organizationName: String,
        projectName: String,
        version: String
    ) = list().filter {
        it.organizationName == organizationName && it.projectName == projectName && it.version == version
    }
        .map { download(it).collectToFile(tempDir / it.fileName).block() }
        .collectList()

    /**
     * Get all the files of requested [version] of [organizationName]/[projectName] demo as zip archive
     *
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of the demo
     * @param archiveName name that an archive should have
     * @return [Flux] with zip as content
     */
    @OptIn(ExperimentalPathApi::class)
    fun archive(
        organizationName: String,
        projectName: String,
        version: String,
        archiveName: String = "archive.zip",
    ): Flux<ByteBuffer> = createTempDirectory().div("archive").createDirectory().let { tempDir ->
        createArchive(tempDir, organizationName, projectName, version, archiveName).doOnComplete {
            tempDir.deleteRecursively()
        }
            .doOnError { tempDir.deleteRecursively() }
    }

    private fun createArchive(
        tmpDir: Path,
        organizationName: String,
        projectName: String,
        version: String,
        archiveName: String = "archive.zip",
    ): Flux<ByteBuffer> =
            downloadToTempDir(tmpDir, organizationName, projectName, version)
                .map {
                    tmpDir.parent.div(archiveName)
                        .also { dirToZip -> tmpDir.compressAsZipTo(dirToZip) }
                }
                .flatMapMany { it.toByteBufferFlux() }
}
