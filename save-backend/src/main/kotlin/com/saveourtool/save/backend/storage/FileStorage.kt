package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.storage.AbstractFileBasedStorage
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.extra.math.max
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name

/**
 * Storage for evaluated tools are loaded by users
 */
@RestController
@RequestMapping("/files")
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

    @PostMapping("/list")
    override fun list(@RequestBody projectCoordinates: ProjectCoordinates?): Flux<FileKey> {
        return super.list(projectCoordinates)
    }

    @PostMapping("/does-exist", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun exists(
        @RequestPart("projectCoordinates") projectCoordinates: ProjectCoordinates?,
        @RequestPart("key") key: FileKey
    ): Mono<Boolean> {
        return super.exists(projectCoordinates, key)
    }

    @PostMapping("/content-size")
    override fun contentSize(projectCoordinates: ProjectCoordinates?, key: FileKey): Mono<Long> {
        return super.contentSize(projectCoordinates, key)
    }

    @PostMapping("/delete")
    override fun delete(projectCoordinates: ProjectCoordinates?, key: FileKey): Mono<Boolean> {
        return super.delete(projectCoordinates, key)
    }

    @PostMapping("/upload")
    override fun upload(projectCoordinates: ProjectCoordinates?, key: FileKey, content: Flux<ByteBuffer>): Mono<Long> {
        return super.upload(projectCoordinates, key, content)
    }

    @PostMapping("/download")
    override fun download(projectCoordinates: ProjectCoordinates?, key: FileKey): Flux<ByteBuffer> {
        return super.download(projectCoordinates, key)
    }
}
