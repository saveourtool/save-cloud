package com.saveourtool.save.sandbox.storage

/**
 * @property userId
 * @property type
 * @property fileName
 */
data class SandboxStorageKey(
    val userId: Long,
    val type: SandboxStorageKeyType,
    val fileName: String,
) {
    companion object {
        private const val DEBUG_INFO_FILE_NAME = "file_name.txt"

        /**
         * create key for DebugInfoKey
         *
         * @param userId
         * @return [SandboxStorageKey] with type [SandboxStorageKeyType.DEBUG_INFO]
         */
        fun debugInfoKey(
            userId: Long
        ): SandboxStorageKey = SandboxStorageKey(
            userId,
            SandboxStorageKeyType.DEBUG_INFO,
            DEBUG_INFO_FILE_NAME
        )
    }
}
