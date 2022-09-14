/**
 * This class contains util methods for Spring
 */

package com.saveourtool.save.backend.utils

import com.saveourtool.save.domain.EntitySaveStatus
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * @return [ResponseEntity] with proper [HttpStatus]
 */
fun EntitySaveStatus.toResponseEntity(): ResponseEntity<EntitySaveStatus> = when (this) {
    EntitySaveStatus.CONFLICT, EntitySaveStatus.EXIST -> ResponseEntity.status(HttpStatus.CONFLICT).body(this)
    EntitySaveStatus.NEW, EntitySaveStatus.UPDATED -> ResponseEntity.ok(this)
    else -> throw NotImplementedError("Not supported save status $this")
}
