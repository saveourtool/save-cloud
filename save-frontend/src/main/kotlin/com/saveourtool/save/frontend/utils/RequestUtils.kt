/**
 * Kotlin/JS utilities for Fetch API
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.http.HttpStatusException
import com.saveourtool.save.v1

import org.w3c.fetch.Headers
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import react.useContext
import react.useEffect
import react.useState

import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

val apiUrl = "${window.location.origin}/api/$v1"

val jsonHeaders = Headers().apply {
    set("Accept", "application/json")
    set("Content-Type", "application/json")
}

/**
 * Interface for objects that have access to [requestStatusContext]
 */
interface WithRequestStatusContext {
    /**
     * Coroutine used for processing [setLoadingCounter]
     */
    val coroutineScope: CoroutineScope

    /**
     * @param response
     */
    fun setResponse(response: Response)

    /**
     * @param isNeedRedirect
     * @param response
     */
    fun setRedirectToFallbackView(isNeedRedirect: Boolean, response: Response)

    /**
     * @param transform
     */
    fun setLoadingCounter(transform: (oldValue: Int) -> Int)
}

/**
 * Get errors from backend (Spring Boot returns errors in message part of json)
 *
 * @return message part of json response
 */
suspend fun Response.unpackMessage(): String = json().await().asDynamic()["message"].toString()

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
suspend fun ComponentWithScope<*, *>.get(
    url: String,
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
) = request(url, "GET", headers, loadingHandler = loadingHandler, responseHandler = responseHandler)

/**
 * Perform POST request from a class component. See [request] for parameter description.
 *
 * @return [Response]
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG")
suspend fun ComponentWithScope<*, *>.post(
    url: String,
    headers: Headers,
    body: dynamic,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
) = request(url, "POST", headers, body, loadingHandler = loadingHandler, responseHandler = responseHandler)

/**
 * Perform DELETE request from a class component. See [request] for parameter description.
 *
 * @return [Response]
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG")
suspend fun ComponentWithScope<*, *>.delete(
    url: String,
    headers: Headers,
    body: dynamic,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
) = request(url, "DELETE", headers, body, loadingHandler = loadingHandler, responseHandler = responseHandler)

/**
 * Perform GET request from a functional component
 *
 * @return [Response]
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS", "KDOC_WITHOUT_PARAM_TAG")
suspend fun WithRequestStatusContext.get(
    url: String,
    headers: Headers,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
) = request(url, "GET", headers, loadingHandler = loadingHandler, responseHandler = responseHandler)

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
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
) = request(url, "POST", headers, body, loadingHandler = loadingHandler, responseHandler = responseHandler)

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
    loadingHandler: suspend (suspend () -> Response) -> Response,
    errorHandler: (Response) -> Unit = this::withModalResponseHandler,
) = request(url, "DELETE", headers, body, loadingHandler = loadingHandler, responseHandler = errorHandler)

/**
 * Handler that allows to show loading modal
 *
 * @param request REST API method
 * @return [Response] received with [request]
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
suspend fun WithRequestStatusContext.loadingHandler(request: suspend () -> Response) = run {
    setLoadingCounter { it + 1 }
    val deferred = coroutineScope.async { request() }
    deferred.invokeOnCompletion {
        setLoadingCounter { it - 1 }
    }
    deferred.await()
}

/**
 * @return true if given [Response] has 409 code, false otherwise
 */
@Suppress("MAGIC_NUMBER")
fun Response.isConflict(): Boolean = this.status == 409.toShort()

/**
 * If this component has context, set [response] in this context. Otherwise, fallback to redirect.
 *
 * @param response
 */
@Suppress("MAGIC_NUMBER")
internal fun ComponentWithScope<*, *>.classComponentResponseHandler(
    response: Response,
) {
    val hasResponseContext = this.asDynamic().context is RequestStatusContext
    if (hasResponseContext) {
        this.withModalResponseHandler(response, false)
    }
}

/**
 * @param response
 */
internal fun ComponentWithScope<*, *>.classComponentRedirectOnFallbackResponseHandler(
    response: Response,
) {
    val hasResponseContext = this.asDynamic().context is RequestStatusContext
    if (hasResponseContext) {
        this.withModalResponseHandler(response, true)
    }
}

/**
 * Handler that allows to show loading modal
 *
 * @param request REST API method
 * @return [Response] received with [request]
 */
@Suppress("MAGIC_NUMBER")
internal suspend fun ComponentWithScope<*, *>.classLoadingHandler(request: suspend () -> Response): Response {
    val hasRequestStatusContext = this.asDynamic().context is RequestStatusContext
    if (hasRequestStatusContext) {
        return this.loadingHandler(request)
    }
    return request()
}

/**
 * If this component has context, set [response] in this context. Otherwise, fallback to redirect.
 *
 * @param response
 */
@Suppress("MAGIC_NUMBER")
internal fun ComponentWithScope<*, *>.classComponentResponseHandlerWithValidation(
    response: Response,
) {
    val hasResponseContext = this.asDynamic().context is RequestStatusContext
    if (hasResponseContext) {
        this.responseHandlerWithValidation(response)
    }
}

/**
 * @param response
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS", "MAGIC_NUMBER")
internal fun WithRequestStatusContext.responseHandlerWithValidation(
    response: Response,
) {
    if (!response.ok && !response.isConflict()) {
        setResponse(response)
    }
}

/**
 * Handler that allows to show loading modal
 *
 * @param request REST API method
 * @return [Response] received with [request]
 */
private suspend fun ComponentWithScope<*, *>.loadingHandler(request: suspend () -> Response) = run {
    val context: RequestStatusContext = this.asDynamic().context
    context.setLoadingCounter { it + 1 }
    val deferred = scope.async { request() }
    deferred.invokeOnCompletion {
        context.setLoadingCounter { it - 1 }
    }
    deferred.await()
}

@Suppress("MAGIC_NUMBER")
private fun ComponentWithScope<*, *>.withModalResponseHandler(
    response: Response,
    isNeedRedirect: Boolean
) {
    if (!response.ok) {
        val statusContext: RequestStatusContext = this.asDynamic().context
        statusContext.setRedirectToFallbackView(isNeedRedirect && response.status == 404.toShort())
        statusContext.setResponse.invoke(response)
    }
}

@Suppress("EXTENSION_FUNCTION_WITH_CLASS", "MAGIC_NUMBER")
private fun WithRequestStatusContext.withModalResponseHandler(
    response: Response,
) {
    if (!response.ok) {
        setResponse(response)
    }
}

private fun ComponentWithScope<*, *>.responseHandlerWithValidation(
    response: Response,
) {
    if (!response.ok && !response.isConflict()) {
        val statusContext: RequestStatusContext = this.asDynamic().context
        statusContext.setResponse.invoke(response)
    }
}

/**
 * Hook to get callbacks to perform requests in functional components.
 *
 * @param request
 * @return a function to trigger request execution.
 */
fun <R> useDeferredRequest(
    request: suspend WithRequestStatusContext.() -> R,
): () -> Unit {
    val scope = CoroutineScope(Dispatchers.Default)
    val context = useRequestStatusContext()
    val (isSending, setIsSending) = useState(false)
    useEffect(isSending) {
        if (!isSending) {
            return@useEffect
        }
        scope.launch {
            request(context)
            setIsSending(false)
        }.invokeOnCompletion {
            if (it != null && it !is CancellationException) {
                setIsSending(false)
            }
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
    return initiateSending
}

/**
 * Hook to perform requests in functional components.
 *
 * @param dependencies
 * @param request
 */
fun <R> useRequest(
    dependencies: Array<dynamic> = emptyArray(),
    request: suspend WithRequestStatusContext.() -> R,
) {
    val scope = CoroutineScope(Dispatchers.Default)
    val context = useRequestStatusContext()

    useEffect(*dependencies) {
        scope.launch {
            request(context)
        }
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }
}

/**
 * Handler that allows to skip loading modal
 *
 * @param request REST API method
 * @return [Response] received with [request]
 */
internal suspend fun noopLoadingHandler(request: suspend () -> Response) = request()

/**
 * Can be used to explicitly specify, that response will be handled is a custom way
 *
 * @param response
 * @return Unit
 */
internal fun noopResponseHandler(response: Response) = Unit

@Suppress("TOO_LONG_FUNCTION", "MAGIC_NUMBER")
private fun useRequestStatusContext(): WithRequestStatusContext {
    val statusContext = useContext(requestStatusContext)
    val context = object : WithRequestStatusContext {
        override val coroutineScope = CoroutineScope(Dispatchers.Default)
        override fun setResponse(response: Response) = statusContext.setResponse(response)
        override fun setRedirectToFallbackView(isNeedRedirect: Boolean, response: Response) = statusContext.setRedirectToFallbackView(
            isNeedRedirect && response.status == 404.toShort()
        )
        override fun setLoadingCounter(transform: (oldValue: Int) -> Int) = statusContext.setLoadingCounter(transform)
    }
    return context
}

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
private suspend fun request(
    url: String,
    method: String,
    headers: Headers,
    body: dynamic = undefined,
    credentials: RequestCredentials? = undefined,
    loadingHandler: suspend (suspend () -> Response) -> Response,
    responseHandler: (Response) -> Unit = ::noopResponseHandler,
): Response =
        loadingHandler {
            window.fetch(
                input = url,
                RequestInit(
                    method = method,
                    headers = headers,
                    body = body,
                    credentials = credentials,
                )
            )
                .await()
        }
            .also { response ->
                if (responseHandler != undefined) {
                    responseHandler(response)
                }
            }
