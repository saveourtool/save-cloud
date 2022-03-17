package org.cqfn.save.api

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
//import org.cqfn.save.core.result.DebugInfo
//import org.cqfn.save.core.result.Pass
//import org.cqfn.save.domain.TestResultDebugInfo
//import org.cqfn.save.domain.TestResultLocation

// TODO move into properties file
private val backendUrl = "http://localhost:5000/internal"
private val preprocessorUrl = "http://localhost:5200"


class AutomaticTestInitializator {
    @OptIn(InternalAPI::class)
    suspend fun start() {
        val httpClient = HttpClient()

        println("-------------------Start post debug info---------------------")

//        val testResultDebugInfo  = TestResultDebugInfo(
//            TestResultLocation(
//                "stub",
//                "stub",
//                "stub",
//                "stub",
//            ),
//            DebugInfo(
//                "stub",
//                "stub",
//                "stub",
//                42
//            ),
//            Pass(
//               "ok",
//               "ok",
//            )
//        )
//        httpClient.post<HttpResponse> {
//            url("${backendUrl}/internal/files/debug-info?agentId=test-agent-id")
//            contentType(ContentType.Application.Json)
//            body = testResultDebugInfo
//        }

        println("-------------------Start execution---------------------")

        httpClient.submitForm<HttpResponse> (
            url = "${backendUrl}/submitExecutionRequest",
            formParameters = Parameters.build {
                append("first_name", "John")
                append("last_name", "Doe")
            }
        )


    }
}