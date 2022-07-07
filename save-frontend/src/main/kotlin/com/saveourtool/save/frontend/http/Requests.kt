/**
 * Methods to make specific requests to backend
 */

package com.saveourtool.save.frontend.http

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo

import org.w3c.fetch.Headers
import org.w3c.fetch.Response

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @param testExecutionDto
 */
suspend fun ComponentWithScope<*, *>.getDebugInfoFor(testExecutionDto: TestExecutionDto) =
        getDebugInfoFor(testExecutionDto, this::post)

/**
 * @param testExecutionDto
 */
suspend fun ComponentWithScope<*, *>.getExecutionInfoFor(testExecutionDto: TestExecutionDto) =
    getExecutionInfoFor(testExecutionDto, this::post)

/**
 * @param testExecutionDto
 */
suspend fun WithRequestStatusContext.getDebugInfoFor(testExecutionDto: TestExecutionDto) =
        getDebugInfoFor(testExecutionDto, this::post)

/**
 * @param name
 * @param organizationName
 * @return project
 */
suspend fun ComponentWithScope<*, *>.getProject(name: String, organizationName: String) = get(
    "$apiUrl/projects/get/organization-name?name=$name&organizationName=$organizationName",
    Headers().apply {
        set("Accept", "application/json")
    },
    loadingHandler = ::classLoadingHandler,
)
    .runCatching {
        decodeFromJsonString<Project>()
    }

/**
 * @param name organization name
 * @return organization
 */
suspend fun ComponentWithScope<*, *>.getOrganization(name: String) = get(
    "$apiUrl/organization/$name",
    Headers().apply {
        set("Accept", "application/json")
    },
    loadingHandler = ::classLoadingHandler,
)
    .decodeFromJsonString<Organization>()

/**
 * @param name username
 * @return info about user
 */
suspend fun ComponentWithScope<*, *>.getUser(name: String) = get(
    "$apiUrl/users/$name",
    Headers().apply {
        set("Accept", "application/json")
    },
    loadingHandler = ::classLoadingHandler,
)
    .decodeFromJsonString<UserInfo>()

/**
 * Fetch debug info for test execution
 *
 * @param testExecutionDto
 * @param post
 * @return Response
 */
@Suppress("TYPE_ALIAS")
private suspend fun getDebugInfoFor(
    testExecutionDto: TestExecutionDto,
    post: suspend (String, Headers, dynamic, suspend (suspend () -> Response) -> Response, (Response) -> Unit) -> Response,
) = post(
    "$apiUrl/files/get-debug-info",
    Headers().apply {
        set("Content-Type", "application/json")
    },
    Json.encodeToString(testExecutionDto),
    ::noopLoadingHandler,
    ::noopResponseHandler
)

/**
 * Fetch execution info for test execution
 *
 * @param testExecutionDto
 * @param post
 * @return Response
 */
@Suppress("TYPE_ALIAS")
private suspend fun getExecutionInfoFor(
    testExecutionDto: TestExecutionDto,
    post: suspend (String, Headers, dynamic, suspend (suspend () -> Response) -> Response) -> Response,
) = post(
    "$apiUrl/files/get-execution-info",
    Headers().apply {
        set("Content-Type", "application/json")
    },
    Json.encodeToString(testExecutionDto),
    ::noopLoadingHandler
)
