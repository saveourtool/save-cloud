package com.saveourtool.save.gateway.controller

import com.saveourtool.save.authservice.utils.SaveUserDetails
import com.saveourtool.save.gateway.service.SaveUserDetailsCache
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller that operates with security info on Gateway
 */
@RestController
@RequestMapping("/internal/sec")
class SecurityInfoInternalController(
    private val saveUserDetailsCache: SaveUserDetailsCache,
) {
    /**
     * Endpoint that saves [saveUserDetails] in local cache
     *
     * @param saveUserDetails
     */
    @PatchMapping("/update")
    fun saveSaveUserDetails(
        @RequestBody saveUserDetails: SaveUserDetails,
    ): Unit = saveUserDetailsCache.save(saveUserDetails)
}
