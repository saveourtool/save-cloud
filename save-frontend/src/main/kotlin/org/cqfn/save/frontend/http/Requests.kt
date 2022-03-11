/**
 * Methods to make specific requests to backend
 */

package org.cqfn.save.frontend.http

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.frontend.utils.WithRequestStatusContext
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.post

import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.Component

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @param testExecutionDto
 */
suspend fun Component<*, *>.getDebugInfoFor(testExecutionDto: TestExecutionDto) =
        getDebugInfoFor(this::post, testExecutionDto)

/**
 * @param testExecutionDto
 */
suspend fun WithRequestStatusContext.getDebugInfoFor(testExecutionDto: TestExecutionDto) =
        getDebugInfoFor(this::post, testExecutionDto)

/**
 * Fetch debug info for test execution
 *
 * @param testExecutionDto
 * @param post
 * @return Response
 */
suspend fun getDebugInfoFor(post: suspend (String, Headers, dynamic) -> Response, testExecutionDto: TestExecutionDto) = post(
    "$apiUrl/files/get-debug-info",
    Headers().apply {
        set("Content-Type", "application/json")
    },
    Json.encodeToString(testExecutionDto)
)
