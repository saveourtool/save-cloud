package com.saveourtool.save.backend.storage

import generated.SAVE_CORE_VERSION

/**
 * @property name
 * @property version
 */
data class InternalFileKey(
    val name: String,
    val version: String,
) {
    companion object {
        private const val LATEST_VERSION = "latest"

        /**
         * [InternalFileKey] for *save-agent*
         */
        val saveAgent: InternalFileKey = InternalFileKey(
            name = "save-agent.kexe",
            version = LATEST_VERSION,
        )

        /**
         * @param version
         * @return [InternalFileKey] for *save-cli* with provided [version]
         */
        fun forSaveCli(version: String = SAVE_CORE_VERSION): InternalFileKey = InternalFileKey(
            name = "save-$version-linuxX64.kexe",
            version = version,
        )
    }
}
