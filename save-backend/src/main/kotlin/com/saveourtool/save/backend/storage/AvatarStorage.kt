package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.AvatarType
import org.springframework.stereotype.Service
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
     * @return key is built from path
     */
    override fun buildKey(rootDir: Path, pathToContent: Path): AvatarKey {
        return AvatarKey(
            if (pathToContent.parent.parent.name == USERS_DIRECTORY) AvatarType.USER else AvatarType.ORGANIZATION,
            pathToContent.parent.name,
            pathToContent.name
        )
    }

    /**
     * @return path to content based on key
     */
    override fun buildPathToContent(rootDir: Path, key: AvatarKey): Path = rootDir
        .let { if (key.type == AvatarType.USER) it.resolve(USERS_DIRECTORY) else it }
        .resolve(key.imageName)
        .resolve(key.filename)

    companion object {
        const val USERS_DIRECTORY = "users"
    }
}

data class AvatarKey(
    val type: AvatarType,
    val imageName: String,
    val filename: String,
) {
    /**
     * Added for backward compatibility
     * @return relative path to avatar image
     */
    fun getRelativePath(): String = when (type) {
        AvatarType.ORGANIZATION -> "/$imageName/$filename"
        AvatarType.USER -> "/${AvatarStorage.USERS_DIRECTORY}/$imageName/$filename"
    }
}
