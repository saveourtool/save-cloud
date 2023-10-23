/**
 * Utilities for Kotlin
 */

package com.saveourtool.save.utils

import org.springframework.http.ResponseEntity
import java.util.concurrent.CompletableFuture

typealias EmptyResponse = ResponseEntity<Void>
typealias StringResponse = ResponseEntity<String>
typealias StringListResponse = ResponseEntity<List<String>>

typealias ListCompletableFuture<T> = CompletableFuture<List<T>>
