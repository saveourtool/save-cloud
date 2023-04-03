@file:JvmName("HttpUtils")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.api.http

import com.saveourtool.save.api.errors.ConnectError
import com.saveourtool.save.api.errors.HttpError
import com.saveourtool.save.api.errors.SaveCloudError
import com.saveourtool.save.api.errors.UnknownHostError
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.utils.io.ByteReadChannel
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * HTTP GET response codes which are considered successful.
 */
val allowedGetResponseCodes = listOf(OK)

internal val allowedPostResponseCodes = listOf(OK, Accepted)

private val allowedDeleteResponseCodes = listOf(OK)

/**
 * Executes an HTTP GET request and checks the response code.
 *
 * @param block the HTTP GET request headers and body.
 * @return the HTTP response body, or the error if an error has occurred.
 */
suspend inline fun <reified T> HttpClient.getAndCheck(
    crossinline block: HttpRequestBuilder.() -> Unit
): Either<SaveCloudError, T> =
        request {
            get(block)
        }.flatMap { response ->
            when (response.status) {
                in allowedGetResponseCodes -> response.body<T>().right()

                else -> HttpError(response).left()
            }
        }

/**
 * Executes an HTTP GET request and opens a read channel to the response body.
 *
 * @param block the HTTP GET request headers and body.
 * @return the read channel to the HTTP response body, or the error if an error
 *   has occurred.
 */
suspend fun HttpClient.getAndOpenChannel(
    block: HttpRequestBuilder.() -> Unit
): Either<SaveCloudError, ByteReadChannel> =
        request {
            get(block)
        }.flatMap { response ->
            when (response.status) {
                in allowedGetResponseCodes -> response.bodyAsChannel().right()

                else -> HttpError(response).left()
            }
        }

/**
 * Executes an HTTP POST request and checks the response code.
 *
 * @param block the HTTP POST request headers and body.
 * @return the HTTP response body, or the error if an error has occurred.
 */
internal suspend inline fun <reified T> HttpClient.postAndCheck(
    crossinline block: HttpRequestBuilder.() -> Unit
): Either<SaveCloudError, T> =
        request {
            post(block)
        }.flatMap { response ->
            when (response.status) {
                in allowedPostResponseCodes -> response.body<T>().right()

                else -> HttpError(response).left()
            }
        }

/**
 * Executes an HTTP DELETE request and checks the response code.
 *
 * @param block the HTTP DELETE request headers and body.
 * @return the HTTP response body, or the error if an error has occurred.
 */
internal suspend inline fun <reified T> HttpClient.deleteAndCheck(
    crossinline block: HttpRequestBuilder.() -> Unit
): Either<SaveCloudError, T> =
        request {
            delete(block)
        }.flatMap { response ->
            when (response.status) {
                in allowedDeleteResponseCodes -> response.body<T>().right()

                else -> HttpError(response).left()
            }
        }

/**
 * Executes an HTTP request.
 *
 * @param block the HTTP request.
 * @return the HTTP response body, or the error if an error has occurred.
 */
suspend fun request(block: suspend () -> HttpResponse): Either<SaveCloudError, HttpResponse> =
        try {
            block().right()
        } catch (ce: ConnectException) {
            ConnectError(ce.message.orEmpty()).left()
        } catch (uhe: UnknownHostException) {
            UnknownHostError(uhe.message.orEmpty()).left()
        }
