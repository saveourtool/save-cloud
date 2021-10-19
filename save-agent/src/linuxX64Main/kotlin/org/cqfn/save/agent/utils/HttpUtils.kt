package org.cqfn.save.agent.utils

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.cqfn.save.agent.AgentState
import org.cqfn.save.agent.RetryConfig
import org.cqfn.save.agent.SaveAgent
import org.cqfn.save.core.logging.logError

/**
 * Attempt to send execution data to backend, will retry several times, while increasing delay 2 times on each iteration.
 */
internal suspend fun SaveAgent.sendDataToBackend(
    retryConfig: RetryConfig,
    requestToBackend: suspend () -> HttpResponse
) = coroutineScope {
    var retryInterval = retryConfig.initialRetryMillis
    repeat(retryConfig.attempts) { attempt ->
        val result = runCatching {
            requestToBackend()
        }
        if (result.isSuccess && result.getOrNull()?.status == HttpStatusCode.OK) {
            return@coroutineScope
        } else {
            val reason = if (result.isSuccess && result.getOrNull()?.status != HttpStatusCode.OK) {
                state.value = AgentState.BACKEND_FAILURE
                "Backend returned status ${result.getOrNull()?.status}"
            } else {
                state.value = AgentState.BACKEND_UNREACHABLE
                "Backend is unreachable, ${result.exceptionOrNull()?.message}"
            }
            logError("Cannot post data (x${attempt + 1}), will retry in $retryInterval second. Reason: $reason")
            delay(retryInterval)
            retryInterval *= 2
        }
    }
}
