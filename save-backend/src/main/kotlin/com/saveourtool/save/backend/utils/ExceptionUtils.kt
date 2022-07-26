/**
 * This file contains util methods to throw exceptions
 */

package com.saveourtool.save.backend.utils

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * @param messageCreator creates message for [ResponseStatusException]
 * @return current object or throw [ResponseStatusException] with status [HttpStatus.NOT_FOUND] when object is null
 * @throws ResponseStatusException when object is null
 */
fun <T> T?.orNotFound(messageCreator: (() -> String?) = { null }): T =
        this ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, messageCreator())
