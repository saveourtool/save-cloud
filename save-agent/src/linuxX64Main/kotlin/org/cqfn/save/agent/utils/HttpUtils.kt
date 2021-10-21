/**
 * Utility methods for HTTP requests
 */

package org.cqfn.save.agent.utils

import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.RetryConfig
import org.cqfn.save.agent.SaveAgent
import org.cqfn.save.core.logging.logError

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * Attempt to send execution data to backend, will retry several times, while increasing delay 2 times on each iteration.
 *
 * @param requestToBackend
 */
internal suspend fun SaveAgent.sendDataToBackend(
    requestToBackend: suspend () -> HttpResponse
): Unit = sendWithRetries(config.retry, requestToBackend) { result, attempt ->
    val reason = if (result.isSuccess && result.getOrNull()?.status != HttpStatusCode.OK) {
        state.value = AgentState.BACKEND_FAILURE
        "Backend returned status ${result.getOrNull()?.status}"
    } else {
        state.value = AgentState.BACKEND_UNREACHABLE
        "Backend is unreachable, ${result.exceptionOrNull()?.message}"
    }
    logError("Cannot post data (x${attempt + 1}), will retry in ${config.retry.initialRetryMillis} ms. Reason: $reason")
}

/**
 * @param retryConfig
 * @param request
 * @param onError
 */
@Suppress("TYPE_ALIAS")
internal suspend fun sendWithRetries(
    retryConfig: RetryConfig,
    request: suspend () -> HttpResponse,
    onError: (Result<HttpResponse>, attempt: Int) -> Unit,
): Unit = coroutineScope {
    var retryInterval = retryConfig.initialRetryMillis
    repeat(retryConfig.attempts) { attempt ->
        val result = runCatching {
            request()
        }
        if (result.isSuccess && result.getOrNull()?.status == HttpStatusCode.OK) {
            return@coroutineScope
        } else {
            onError(result, attempt)
            delay(retryInterval)
            retryInterval *= 2
        }
    }
}
