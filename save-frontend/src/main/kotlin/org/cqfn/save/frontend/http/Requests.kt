/**
 * Methods to make specific requests to backend
 */

package com.saveourtool.save.frontend.http

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.utils.WithRequestStatusContext
import com.saveourtool.save.frontend.utils.apiUrl
import com.saveourtool.save.frontend.utils.decodeFromJsonString
import com.saveourtool.save.frontend.utils.get
import com.saveourtool.save.frontend.utils.post
import com.saveourtool.save.info.UserInfo

import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.Component

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @param testExecutionDto
 */
suspend fun Component<*, *>.getDebugInfoFor(testExecutionDto: TestExecutionDto) =
        getDebugInfoFor(testExecutionDto, this::post)

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
suspend fun Component<*, *>.getProject(name: String, organizationName: String) = get(
    "$apiUrl/projects/get/organization-name?name=$name&organizationName=$organizationName",
    Headers().apply {
        set("Accept", "application/json")
    },
)
    .runCatching {
        decodeFromJsonString<Project>()
    }

/**
 * @param name organization name
 * @return organization
 */
suspend fun Component<*, *>.getOrganization(name: String) = get(
    "$apiUrl/organization/$name",
    Headers().apply {
        set("Accept", "application/json")
    },
)
    .decodeFromJsonString<Organization>()

/**
 * @param name username
 * @return info about user
 */
suspend fun Component<*, *>.getUser(name: String) = get(
    "$apiUrl/users/$name",
    Headers().apply {
        set("Accept", "application/json")
    },
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
private suspend fun getDebugInfoFor(testExecutionDto: TestExecutionDto,
                                    post: suspend (String, Headers, dynamic) -> Response,
) = post(
    "$apiUrl/files/get-debug-info",
    Headers().apply {
        set("Content-Type", "application/json")
    },
    Json.encodeToString(testExecutionDto)
)
