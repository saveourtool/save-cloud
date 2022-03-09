/**
 * Kotlin/JS utilities for Fetch API
 */

package org.cqfn.save.frontend.utils

import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project

import org.w3c.fetch.Headers
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import react.Component
import react.StateSetter

val apiUrl = "${window.location.origin}/api"

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

suspend fun get(url: String, headers: Headers,
                responseHandler: (Response) -> Unit = ::redirectResponseHandler,
) = request(url, "GET", headers, responseHandler = responseHandler)

suspend fun Component<*, *>.get(url: String, headers: Headers,
                      responseHandler: (Response) -> Unit = this::classComponentResponseHandler,
) = request(url, "GET", headers, responseHandler = responseHandler)

suspend fun post(url: String, headers: Headers, body: dynamic,
                 responseHandler: (Response) -> Unit = ::redirectResponseHandler,) =
    request(url, "POST", headers, body, responseHandler = responseHandler)

suspend fun Component<*, *>.post(url: String, headers: Headers, body: dynamic,
                     responseHandler: (Response) -> Unit = this::classComponentResponseHandler,) =
    request(url, "POST", headers, body, responseHandler = responseHandler)

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
        if (responseHandler != undefined) responseHandler(response)
    }

/*suspend fun useRequest(method: String, path: String, body: dynamic): Response {
    val setErrorCode = useContext(errorStatusContext)

    val response = Any().request(path, method, Headers(), body)
    if (!response.ok) {
        setErrorCode(response.status.toInt())
    }
    return response
}*/

internal fun redirectResponseHandler(response: Response) {
    if (response.status == 401.toShort()) {
        // if 401 - change current URL to the main page (with login screen)
        // note: we may have other uses for 401 in the future
        window.location.href = "${window.location.origin}/#"
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

/**
 * Can be used to explicitly specify, that response will be handled is a custom way
 */
internal fun noopResponseHandler(response: Response) = Unit

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
suspend fun Component<*, *>.getOrganization(name: String) =     get(
    "$apiUrl/organization/get/organization-name?name=$name",
    Headers().apply {
            set("Accept", "application/json")
        },
            responseHandler = ::withModalResponseHandler,
        )
            .decodeFromJsonString<Organization>()
