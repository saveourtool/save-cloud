/**
 * Methods to make specific requests to backend
 */

package org.cqfn.save.frontend.http

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.utils.WithRequestStatusContext
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.info.UserInfo

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
 * @return info about user's
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
