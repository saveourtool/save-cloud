package com.saveourtool.save.gateway.service

import com.saveourtool.save.authservice.utils.SaveUserDetails
import org.springframework.stereotype.Component
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Cache for [SaveUserDetails]
 */
@Component
class SaveUserDetailsCache {
    private val cache = hashMapOf<Long, SaveUserDetails>()
    private val reentrantReadWriteLock = ReentrantReadWriteLock()

    /**
     * @param id [SaveUserDetails.id]
     * @return cached [SaveUserDetails] or null
     */
    fun get(id: Long): SaveUserDetails? = reentrantReadWriteLock.read {
        cache[id]
    }

    /**
     * Caches provided [saveUserDetails]
     *
     * @param saveUserDetails [SaveUserDetails]
     */
    fun save(saveUserDetails: SaveUserDetails): Unit = reentrantReadWriteLock.write {
        cache[saveUserDetails.id] = saveUserDetails
    }
}
