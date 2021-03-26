package org.cqfn.save.frontend.utils

import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

import kotlinx.browser.window
import kotlinx.coroutines.await

suspend fun get(url: String, headers: Headers) = request(url, "GET", headers)

suspend fun post(url: String, headers: Headers, body: dynamic) = request(url, "GET", headers, body)

suspend fun request(url: String,
                    method: String,
                    headers: Headers,
                    body: dynamic = undefined,
) = window.fetch(
    input = url,
    RequestInit(
        method = method,
        headers = headers,
        body = body,
    )
)
    .await()