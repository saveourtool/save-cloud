/**
 * Utilities for testing of storage logic
 */
package com.saveourtool.save.backend.utils

import com.saveourtool.save.storage.Storage
import com.saveourtool.save.storage.StorageProjectReactor
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Mock [Storage.usingProjectReactor]
 *
 * @param storageProjectReactor
 */
fun <K: Any, S: StorageProjectReactor<K>> Storage<K>.mockUsingStorageProjectReactor(
    storageProjectReactor: S,
) {
    whenever(usingProjectReactor())
        .thenReturn(storageProjectReactor)
    whenever(usingProjectReactor(any<StorageProjectReactor<K>.() -> Any>()))
        .thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            invocation.arguments
                .first()
                .let {
                    it as StorageProjectReactor<K>.() -> Any
                }
                .let {
                    it(storageProjectReactor)
                }
        }
}
