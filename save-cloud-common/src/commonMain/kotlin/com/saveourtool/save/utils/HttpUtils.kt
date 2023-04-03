/**
 * Utils to check results of http requests
 */

package com.saveourtool.save.utils

import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * @return true if [HttpResponse] is not ok or some failure has happened, false otherwise
 */
fun Result<HttpResponse>.failureOrNotOk() = isFailure || notOk()

/**
 * @return true if [HttpResponse] is not successful, but [Result] is completed, false otherwise
 */
fun Result<HttpResponse>.notOk() = isSuccess && !getOrThrow().status.isSuccess()
