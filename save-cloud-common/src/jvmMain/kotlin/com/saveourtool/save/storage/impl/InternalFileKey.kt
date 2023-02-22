package com.saveourtool.save.storage.impl

/**
 * @property name
 * @property version
 */
data class InternalFileKey(
    val name: String,
    val version: String,
) {
    /**
     * @return true if version is latest, otherwise -- false
     */
    fun isLatest(): Boolean = version == LATEST_VERSION

    companion object {
        private const val LATEST_VERSION = "latest"

        /**
         * @param name
         * @return [InternalFileKey] with latest version
         */
        fun latest(name: String): InternalFileKey = InternalFileKey(name, LATEST_VERSION)

        /**
         * [InternalFileKey] for *save-agent*
         */
        val saveAgentKey: InternalFileKey = latest("save-agent.kexe")

        /**
         * @param version
         * @return [InternalFileKey] for *save-cli* with version [version]
         */
        fun saveCliKey(version: String): InternalFileKey = InternalFileKey(
            name = "save-$version-linuxX64.kexe",
            version = version,
        )
    }
}
