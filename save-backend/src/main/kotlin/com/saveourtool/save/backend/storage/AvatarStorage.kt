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
        when (pathToContent.parent.name) {
            USERS_DIRECTORY -> AvatarType.USER
            ORGANIZATIONS_DIRECTORY -> AvatarType.ORGANIZATION
            else -> AvatarType.NONE
        },
        pathToContent.name,
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
        .let {
            when (key.type) {
                AvatarType.USER -> it.resolve(USERS_DIRECTORY)
                AvatarType.ORGANIZATION -> it.resolve(ORGANIZATIONS_DIRECTORY)
                else -> throw IllegalStateException("Not supported type: ${key.type}")
            }
        }
        .resolve(key.objectName)

    companion object {
        const val ORGANIZATIONS_DIRECTORY = "organizations"
        const val USERS_DIRECTORY = "users"
    }
}

/**
 * @property type
 * @property objectName
 */
data class AvatarKey(
    val type: AvatarType,
    val objectName: String,
) {
    /**
     * Added for backward compatibility
     *
     * @return relative path to avatar image
     */
    fun getRelativePath(): String = when (type) {
        AvatarType.ORGANIZATION -> "/${AvatarStorage.ORGANIZATIONS_DIRECTORY}/$objectName"
        AvatarType.USER -> "/${AvatarStorage.USERS_DIRECTORY}/$objectName"
        else -> throw IllegalStateException("Not supported type: $type")
    }
}
