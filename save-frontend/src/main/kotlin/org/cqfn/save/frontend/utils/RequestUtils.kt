/**
 * Kotlin/JS utilities for Fetch API
 */

package org.cqfn.save.frontend.utils

import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
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

fun interface WithRequestStatusContext {
    /**
     * @param code
     */
    fun setErrorCode(code: Int)
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
 * @param url
 * @param headers
 * @param responseHandler
 * @return
 */
suspend fun Component<*, *>.get(url: String, headers: Headers,
                                responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
) = request(url, "GET", headers, responseHandler = responseHandler)

/**
 * @param url
 * @param headers
 * @param body
 * @param responseHandler
 * @return
 */
suspend fun Component<*, *>.post(
    url: String,
    headers: Headers,
    body: dynamic,
    responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
) = request(url, "POST", headers, body, responseHandler = responseHandler)

/**
 * @param name
 * @param organizationName
 * @return project
 */
suspend fun Component<*, *>.getProject(name: String, organizationName: String) = get(
    "$apiUrl/projects/get/organization-name?name=$name&organizationName=$organizationName",
    Headers().apply {
        set("Accept", "application/json")
    },
    responseHandler = ::withModalResponseHandler,
)
    .runCatching {
        decodeFromJsonString<Project>()
    }

/**
 * @param name organization name
 * @return organization
 */
suspend fun Component<*, *>.getOrganization(name: String) = get(
    "$apiUrl/organization/get/organization-name?name=$name",
    Headers().apply {
        set("Accept", "application/json")
    },
    responseHandler = ::withModalResponseHandler,
)
    .decodeFromJsonString<Organization>()

/**
 * @param url
 * @param headers
 * @param body
 * @param responseHandler
 * @return
 */
suspend fun WithRequestStatusContext.post(
    url: String,
    headers: Headers,
    body: dynamic,
    responseHandler: (Response) -> Unit = this::withModalResponseHandler,
) = request(url, "POST", headers, body, responseHandler = responseHandler)

/**
 * @param response
 */
internal fun Component<*, *>.classComponentResponseHandler(
    response: Response
) {
    if (this.asDynamic().context is Function<*>) {
        // dirty hack to determine whether this component contains `setErrorCode` in its context.
        // If we add another context with a function, this logic will break.
        console.log("Branch for component class ${this::class} with context=${this.asDynamic().context}")
        this.unsafeCast<Component<*, *>>().withModalResponseHandler(response)
    } else {
        console.log("Default branch for this=${this::class}")
        redirectResponseHandler(response)
    }
}

private fun Component<*, *>.withModalResponseHandler(
    response: Response
) {
    if (!response.ok) {
        val setErrorCode: StateSetter<Int?> = this.asDynamic().context
        setErrorCode(response.status.toInt())
    }
}

private fun WithRequestStatusContext.withModalResponseHandler(
    response: Response
) {
    if (!response.ok) {
        console.log("setErrorCode with code ${response.status}")
        setErrorCode(response.status.toInt())
    }
}

/**
 * @param dependencies
 * @param request
 * @return
 */
fun <R> useRequest(dependencies: Array<dynamic>,
                   request: suspend WithRequestStatusContext.(checkStatus: (Response) -> Unit) -> R,
): () -> Unit {
    val scope = CoroutineScope(Dispatchers.Default)
    val (isSending, setIsSending) = useState(false)
    val setErrorCode = useContext(errorStatusContext)
    val context = WithRequestStatusContext {
        setErrorCode(it)
    }

    useEffect(isSending, *dependencies) {
        console.log("useEffect with isSending=$isSending")
        if (!isSending) {
            return@useEffect
        }
        scope.launch {
            val checkStatus: (Response) -> Unit = { response ->
                if (!response.ok) {
                    setErrorCode(response.status.toInt())
                }
            }
            request(context, checkStatus)
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
 * @return
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
