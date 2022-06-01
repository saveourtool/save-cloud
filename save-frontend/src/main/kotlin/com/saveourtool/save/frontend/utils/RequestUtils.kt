/**
 * Kotlin/JS utilities for Fetch API
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.errorStatusContext
import com.saveourtool.save.frontend.http.HttpStatusException
import com.saveourtool.save.v1

import org.w3c.fetch.Headers
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import react.Component
import react.StateSetter
import react.useContext
import react.useEffect
import react.useState

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

val apiUrl = "${window.location.origin}/api/$v1"

/**
 * Interface for objects that have access to [errorStatusContext]
 */
fun interface WithRequestStatusContext {
    /**
     * @param response
     */
    fun setResponse(response: Response)
}

/**
 * Perform a mapping operation on a [Response] if it's status is OK or throw an exception otherwise.
 *
 * @param map mapping function
 * @return mapped result
 * @throws IllegalStateException if response status is not OK
 */
suspend fun <T> Response.unsafeMap(map: suspend (Response) -> T) = if (this.ok) {
    map(this)
} else {
    throw HttpStatusException(status, statusText)
}

/**
 * Read [this] Response body as text and deserialize it using [Json] as type [T]
 *
 * @return response body deserialized as [T]
 */
suspend inline fun <reified T> Response.decodeFromJsonString() = Json.decodeFromString<T>(text().await())

/**
 * Perform GET request from a class component. See [request] for parameter description.
 *
 * @return [Response]
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG")
suspend fun Component<*, *>.get(url: String,
                                headers: Headers,
                                responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
) = request(url, "GET", headers, responseHandler = responseHandler)

/**
 * Perform POST request from a class component. See [request] for parameter description.
 *
 * @return [Response]
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG")
suspend fun Component<*, *>.post(url: String,
                                 headers: Headers,
                                 body: dynamic,
                                 responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
) = request(url, "POST", headers, body, responseHandler = responseHandler)

/**
 * Perform DELETE request from a class component. See [request] for parameter description.
 *
 * @return [Response]
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG")
suspend fun Component<*, *>.delete(
    url: String,
    headers: Headers,
    body: dynamic,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
) = request(url, "DELETE", headers, body, responseHandler = responseHandler)

/**
 * Perform GET request from a functional component
 *
 * @return [Response]
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS", "KDOC_WITHOUT_PARAM_TAG")
suspend fun WithRequestStatusContext.get(
    url: String,
    headers: Headers,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
) = request(url, "GET", headers, responseHandler = responseHandler)

/**
 * Perform POST request from a functional component
 *
 * @return [Response]
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS", "KDOC_WITHOUT_PARAM_TAG")
suspend fun WithRequestStatusContext.post(
    url: String,
    headers: Headers,
    body: dynamic,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
) = request(url, "POST", headers, body, responseHandler = responseHandler)

/**
 * Perform DELETE request from a functional component
 *
 * @return [Response]
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS", "KDOC_WITHOUT_PARAM_TAG")
suspend fun WithRequestStatusContext.delete(
    url: String,
    headers: Headers,
    body: dynamic,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
) = request(url, "DELETE", headers, body, responseHandler = responseHandler)

/**
 * If this component has context, set [response] in this context. Otherwise, fallback to redirect.
 *
 * @param response
 */
@Suppress("MAGIC_NUMBER")
internal fun Component<*, *>.classComponentResponseHandler(
    response: Response
) {
    // dirty hack to determine whether this component contains `setResponse` in its context.
    // If we add another context with a function, this logic will break.
    val hasResponseContext = this.asDynamic().context is Function<*>
    if (hasResponseContext) {
        this.unsafeCast<Component<*, *>>().withModalResponseHandler(response)
    }
}

private fun Component<*, *>.withModalResponseHandler(response: Response) {
    if (!response.ok) {
        val setResponse: StateSetter<Response?> = this.asDynamic().context
        setResponse(response)
    }
}

@Suppress("EXTENSION_FUNCTION_WITH_CLASS", "MAGIC_NUMBER")
private fun WithRequestStatusContext.withModalResponseHandler(response: Response) {
    if (!response.ok) {
        setResponse(response)
    }
}

/**
 * Hook to perform requests in functional components.
 *
 * @param dependencies
 * @param isDeferred whether this request should be performed right after component is mounted (`isDeferred == false`)
 * or called later by some other mechanism (e.g. a button click).
 * @param request
 * @return a function to trigger request execution. If `isDeferred == false`, this function should be called right after the hook is called.
 */
fun <R> useRequest(dependencies: Array<dynamic> = emptyArray(),
                   isDeferred: Boolean = true,
                   request: suspend WithRequestStatusContext.() -> R,
): () -> Unit {
    val scope = CoroutineScope(Dispatchers.Default)
    val (isSending, setIsSending) = useState(false)
    val setResponse = useContext(errorStatusContext)
    val context = WithRequestStatusContext {
        setResponse(it)
    }

    useEffect(isSending, *dependencies) {
        if (!isSending) {
            return@useEffect
        }
        scope.launch {
            request(context)
            setIsSending(false)
        }
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }

    val initiateSending: () -> Unit = {
        if (!isSending) {
            setIsSending(true)
        }
    }
    @Suppress("BRACES_BLOCK_STRUCTURE_ERROR")
    return if (!isDeferred) { {
        useEffect(*dependencies) { initiateSending() }
    } } else {
        return initiateSending
    }
}

/**
 * Can be used to explicitly specify, that response will be handled is a custom way
 *
 * @param response
 * @return Unit
 */
internal fun noopResponseHandler(response: Response) = Unit

/**
 * Perform an HTTP request using Fetch API. Suspending function that returns a [Response] - a JS promise with result.
 *
 * @param url request URL
 * @param method HTTP request method
 * @param headers HTTP headers
 * @param body request body
 * @param credentials [RequestCredentials] for fetch API
 * @return [Response] instance
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
private suspend fun request(url: String,
                            method: String,
                            headers: Headers,
                            body: dynamic = undefined,
                            credentials: RequestCredentials? = undefined,
                            responseHandler: (Response) -> Unit = ::noopResponseHandler,
): Response = window.fetch(
    input = url,
    RequestInit(
        method = method,
        headers = headers,
        body = body,
        credentials = credentials,
    )
)
    .await().also { response ->
        if (responseHandler != undefined) {
            responseHandler(response)
        }
    }
