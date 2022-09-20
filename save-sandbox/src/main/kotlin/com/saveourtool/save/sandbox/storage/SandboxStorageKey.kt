package com.saveourtool.save.sandbox.storage

/**
 * @property userName
 * @property type
 * @property fileName
 */
data class SandboxStorageKey(
    val userName: String,
    val type: SandboxStorageKeyType,
    val fileName: String,
) {
    companion object {
        private const val DEBUG_INFO_FILE_NAME = "file_name.txt"

        /**
         * create key for DebugInfoKey
         */
        fun debugInfoKey(
            userName: String
        ): SandboxStorageKey = SandboxStorageKey(
            userName,
            SandboxStorageKeyType.DEBUG_INFO,
            DEBUG_INFO_FILE_NAME
        )
    }
}
