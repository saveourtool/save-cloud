/**
 * Utility methods for HTTP requests
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.SaveAgent
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*

import io.ktor.client.statement.HttpResponse
import io.ktor.http.*

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * @param result
 * @return true if [this] agent's state has been updated to reflect problems with [result]
 */
internal fun SaveAgent.updateStateBasedOnBackendResponse(
    result: Result<HttpResponse>
) = if (result.isSuccess && result.getOrNull()?.status != HttpStatusCode.OK) {
    state.value = AgentState.BACKEND_FAILURE
    true
} else if (result.isFailure) {
    state.value = AgentState.BACKEND_UNREACHABLE
    true
} else {
    false
}

/**
 * Attempt to send execution data to backend.
 *
 * @param requestToBackend
 */
internal suspend fun SaveAgent.sendDataToBackend(
    requestToBackend: suspend () -> HttpResponse
): Result<HttpResponse> = runCatching { requestToBackend() }.apply {
    val reason = if (isSuccess && getOrNull()?.status != HttpStatusCode.OK) {
        state.value = AgentState.BACKEND_FAILURE
        "Backend returned status ${getOrNull()?.status}"
    } else {
        state.value = AgentState.BACKEND_UNREACHABLE
        "Backend is unreachable, ${exceptionOrNull()?.message}"
    }
    logErrorCustom("Cannot send data to backed: $reason")
}

/**
 * Perform a POST request to [url] (optionally with body [body] that will be serialized as JSON),
 * accepting application/octet-stream and return result wrapping [HttpResponse]
 *
 * @param url
 * @param body
 * @return result wrapping [HttpResponse]
 */
internal suspend fun HttpClient.download(url: String, body: Any?): Result<HttpResponse> = runCatching {
    post {
        url(url)
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
        body?.let { setBody(it) }
        onDownload { bytesSentTotal, contentLength ->
            logDebugCustom("Received $bytesSentTotal bytes from $contentLength")
        }
    }
}
