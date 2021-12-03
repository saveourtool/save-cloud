/**
 * Methods to make specific requests to backend
 */

package org.cqfn.save.frontend.http

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.frontend.utils.post

import org.w3c.fetch.Headers

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cqfn.save.frontend.utils.apiUrl

/**
 * Fetch debug info for test execution
 *
 * @param testExecutionDto
 * @return Response
 */
suspend fun getDebugInfoFor(testExecutionDto: TestExecutionDto) = post(
    "$apiUrl/files/get-debug-info",
    Headers().apply {
        set("Content-Type", "application/json")
    },
    Json.encodeToString(testExecutionDto)
)
