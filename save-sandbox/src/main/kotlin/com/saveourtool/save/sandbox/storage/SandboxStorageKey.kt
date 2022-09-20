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
)
