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
    }
}
