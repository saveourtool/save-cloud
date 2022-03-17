package org.cqfn.save.api

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*

// TODO move into properties file
private val backendUrl = "http://localhost:5000/internal"
private val preprocessorUrl = "http://localhost:5200"


class AutomaticTestInitializator {
    @OptIn(InternalAPI::class)
    suspend fun start() {
        val httpClient = HttpClient()

        httpClient.submitForm<HttpResponse> (
            url = "${backendUrl}/submitExecutionRequest",
            formParameters = Parameters.build {
                append("first_name", "John")
                append("last_name", "Doe")
            }
        )
    }
}