/**
 * Storage for avatars + key for this storage
 */

package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.AvatarType
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.name

/**
 * Storage for Avatars
 * Currently, key is (AvatarType, ImageName, Filename) -- can be refactored to avoid filename
 */
@Service
class AvatarStorage(configProperties: ConfigProperties) :
    AbstractFileBasedStorage<AvatarKey>(Path.of(configProperties.fileStorage.location) / "images" / "avatars") {
    /**
     * @param rootDir
     * @param pathToContent
     * @return [AvatarKey] object is built by [Path]
     */
    override fun buildKey(rootDir: Path, pathToContent: Path): AvatarKey = AvatarKey(
        if (pathToContent.parent.parent.name == USERS_DIRECTORY) AvatarType.USER else AvatarType.ORGANIZATION,
        pathToContent.parent.name,
        pathToContent.name
    )

    /**
     * @param key
     * @param content
     * @return `Mono` with file size
     */
    fun upsert(key: AvatarKey, content: Flux<ByteBuffer>): Mono<Long> = list()
        .filter { it.objectName == key.objectName }
        .singleOrEmpty()
        .flatMap { delete(it) }
        .switchIfEmpty(Mono.just(true))
        .flatMap { upload(key, content) }

    /**
     * @param rootDir
     * @param key
     * @return [Path] is built by [AvatarKey] object
     */
    override fun buildPathToContent(rootDir: Path, key: AvatarKey): Path = rootDir
        .let { if (key.type == AvatarType.USER) it.resolve(USERS_DIRECTORY) else it }
        .resolve(key.objectName)
        .resolve(key.imageName)

    companion object {
        const val USERS_DIRECTORY = "users"
    }
}

/**
 * @property type
 * @property objectName
 * @property imageName
 */
data class AvatarKey(
    val type: AvatarType,
    val objectName: String,
    val imageName: String,
) {
    /**
     * Added for backward compatibility
     *
     * @return relative path to avatar image
     */
    fun getRelativePath(): String = when (type) {
        AvatarType.ORGANIZATION -> "/$objectName/$imageName"
        AvatarType.USER -> "/${AvatarStorage.USERS_DIRECTORY}/$objectName/$imageName"
    }
}
