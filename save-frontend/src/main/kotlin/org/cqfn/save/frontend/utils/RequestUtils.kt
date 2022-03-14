/**
 * Kotlin/JS utilities for Fetch API
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.utils

import org.cqfn.save.frontend.components.errorStatusContext

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

val apiUrl = "${window.location.origin}/api"

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
    error("$status $statusText")
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
 * Perform GET request from a functional component
 *
 * @return [Response]
 */
@Suppress("EXTENSION_FUNCTION_WITH_CLASS", "KDOC_WITHOUT_PARAM_TAG")
suspend fun WithRequestStatusContext.get(url: String,
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
 * If this component has context, set [response] in this context. Otherwise, fallback to redirect.
 *
 * @param response
 */
internal fun Component<*, *>.classComponentResponseHandler(
    response: Response
) {
    if (this.asDynamic().context is Function<*>) {
        // dirty hack to determine whether this component contains `setResponse` in its context.
        // If we add another context with a function, this logic will break.
        console.log("Branch for component class ${this::class} with context=${this.asDynamic().context}")
        this.unsafeCast<Component<*, *>>().withModalResponseHandler(response)
    } else {
        console.log("Default branch for this=${this::class}")
        redirectResponseHandler(response)
    }
}

private fun Component<*, *>.withModalResponseHandler(response: Response) {
    if (!response.ok) {
        val setResponse: StateSetter<Response?> = this.asDynamic().context
        setResponse(response)
    }
}

@Suppress("EXTENSION_FUNCTION_WITH_CLASS")
private fun WithRequestStatusContext.withModalResponseHandler(response: Response) {
    if (!response.ok) {
        console.log("setResponse with code ${response.status}")
        setResponse(response)
    }
}

/**
 * Hook to perform requests in functional components.
 *
 * @param dependencies
 * @param request
 * @return a function to trigger request execution
 */
fun <R> useRequest(dependencies: Array<dynamic> = emptyArray(),
                   request: suspend WithRequestStatusContext.() -> R,
): () -> Unit {
    val scope = CoroutineScope(Dispatchers.Default)
    val (isSending, setIsSending) = useState(false)
    val setResponse = useContext(errorStatusContext)
    val context = WithRequestStatusContext {
        setResponse(it)
    }

    useEffect(isSending, *dependencies) {
        console.log("useEffect with isSending=$isSending")
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

    return {
        if (!isSending) {
            setIsSending(true)
        }
    }
}

/**
 * @param response
 */
internal fun redirectResponseHandler(response: Response) {
    if (response.status == 401.toShort()) {
        // if 401 - change current URL to the main page (with login screen)
        // note: we may have other uses for 401 in the future
        window.location.href = "${window.location.origin}/#"
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
                            responseHandler: (Response) -> Unit = ::redirectResponseHandler,
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
