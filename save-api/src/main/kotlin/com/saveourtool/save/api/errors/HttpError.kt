package com.saveourtool.save.api.errors

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request

/**
 * An HTTP response with a non-2xx status.
 *
 * @property response the encapsulated response.
 * @property message the detail message.
 */
data class HttpError internal constructor(
    val response: HttpResponse,
    override val message: String
) : SaveCloudError() {
    companion object {
        /**
         * Creates an error from [response].
         *
         * @param response the original HTTP response.
         * @return the newly-created error.
         */
        suspend operator fun invoke(response: HttpResponse): HttpError =
                with(response) {
                    "HTTP $status from ${request.url}; body: ${bodyAsText()}"
                }.let { message ->
                    HttpError(response, message)
                }
    }
}
