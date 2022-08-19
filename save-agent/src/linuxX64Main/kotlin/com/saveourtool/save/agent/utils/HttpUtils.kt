/**
 * Utility methods for HTTP requests
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.agent.AgentState
import com.saveourtool.save.agent.SaveAgent

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

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
