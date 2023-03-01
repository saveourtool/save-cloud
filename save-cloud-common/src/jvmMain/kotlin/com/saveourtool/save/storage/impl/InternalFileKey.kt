package com.saveourtool.save.storage.impl

/**
 * @property name
 * @property version
 */
data class InternalFileKey(
    val name: String,
    val version: String,
    val fileName: String,
) {
    /**
     * @return true if version is latest, otherwise -- false
     */
    fun isLatest(): Boolean = version == LATEST_VERSION

    companion object {
        private const val LATEST_VERSION = "latest"

        /**
         * [Comparator] for [InternalFileKey] which compare by [InternalFileKey.version] taking into account [LATEST_VERSION]
         */
        val versionCompartor: Comparator<InternalFileKey> = Comparator { key1, key2 ->
            require(key1.name == key2.name)
            if (key1.isLatest()) {
                1
            } else if (key2.isLatest()) {
                -1
            } else {
                key1.version.compareTo(key2.version)
            }
        }

        /**
         * [InternalFileKey.name] for *save-agent*
         */
        val saveAgentKeyName: String = "save-agent"

        /**
         * [InternalFileKey] for *save-agent*
         */
        val latestSaveAgentKey: InternalFileKey = latest(saveAgentKeyName, "save-agent.kexe")

        /**
         * [InternalFileKey.name] for *save-cli*
         */
        val saveCliKeyName: String = "save-cli"

        /**
         * [InternalFileKey] for latest *save-cli*
         */
        val latestSaveCliKey: InternalFileKey = latest(saveCliKeyName, "save-linuxX64.kexe")

        /**
         * @param name
         * @param fileName
         * @return [InternalFileKey] with latest version
         */
        fun latest(name: String, fileName: String): InternalFileKey = InternalFileKey(name, LATEST_VERSION, fileName)

        /**
         * @param version
         * @return [InternalFileKey] for *save-cli* with version [version]
         */
        fun saveCliKey(version: String): InternalFileKey = InternalFileKey(
            name = saveCliKeyName,
            version = version,
            fileName = "save-$version-linuxX64.kexe",
        )
    }
}
