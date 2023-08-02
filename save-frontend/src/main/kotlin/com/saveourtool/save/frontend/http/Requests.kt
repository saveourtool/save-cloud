/**
 * Methods to make specific requests to backend
 */

package com.saveourtool.save.frontend.http

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.entities.*
import com.saveourtool.save.entities.contest.ContestDto
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.utils.CONTENT_LENGTH_CUSTOM
import com.saveourtool.save.utils.FILE_PART_NAME
import js.core.jso

import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import web.file.File
import web.http.FormData

import kotlinx.browser.window

/**
 * @param name
 * @param organizationName
 * @return project
 */
suspend fun ComponentWithScope<*, *>.getProject(name: String, organizationName: String) = get(
    "$apiUrl/projects/get/organization-name?name=$name&organizationName=$organizationName",
    jsonHeaders,
    loadingHandler = ::classLoadingHandler,
    responseHandler = ::classComponentRedirectOnFallbackResponseHandler,
)
    .runCatching {
        decodeFromJsonString<ProjectDto>()
    }

/**
 * @param name organization name
 * @return organization
 */
suspend fun ComponentWithScope<*, *>.getOrganization(name: String) = get(
    "$apiUrl/organizations/$name",
    jsonHeaders,
    loadingHandler = ::classLoadingHandler,
    responseHandler = ::classComponentRedirectOnFallbackResponseHandler,
)
    .decodeFromJsonString<OrganizationDto>()

/**
 * @param name contest name
 * @return contestDTO
 */
suspend fun ComponentWithScope<*, *>.getContest(name: String) = get(
    "$apiUrl/contests/$name",
    jsonHeaders,
    loadingHandler = ::classLoadingHandler,
    responseHandler = ::classComponentRedirectOnFallbackResponseHandler,
)
    .decodeFromJsonString<ContestDto>()

/**
 * @param name username
 * @return info about user
 */
suspend fun ComponentWithScope<*, *>.getUser(name: String) = get(
    "$apiUrl/users/$name",
    jsonHeaders,
    loadingHandler = ::classLoadingHandler,
)
    .decodeFromJsonString<UserInfo>()

/**
 * @param file image file
 * @param name avatar owner name
 * @param type avatar type
 * @param loadingHandler
 */
suspend fun ComponentWithScope<*, *>.postImageUpload(
    file: File,
    name: String,
    type: AvatarType,
    loadingHandler: suspend (suspend () -> Response) -> Response,
) {
    val response = post(
        url = "$apiUrl/avatar/upload",
        params = jso<dynamic> {
            owner = name
            this.type = type
        },
        Headers().apply { append(CONTENT_LENGTH_CUSTOM, file.size.toString()) },
        FormData().apply { set(FILE_PART_NAME, file) },
        loadingHandler,
    )
    if (response.ok) {
        window.location.reload()
    }
}

/**
 * @param file image file
 * @param name avatar owner name
 * @param type avatar type
 * @param loadingHandler
 */
suspend fun WithRequestStatusContext.postImageUpload(
    file: File,
    name: String?,
    type: AvatarType,
    loadingHandler: suspend (suspend () -> Response) -> Response,
) {
    val response = post(
        url = "$apiUrl/avatar/upload",
        params = jso<dynamic> {
            owner = name
            this.type = type
        },
        Headers().apply { append(CONTENT_LENGTH_CUSTOM, file.size.toString()) },
        FormData().apply { set(FILE_PART_NAME, file) },
        loadingHandler,
    )
    if (response.ok) {
        window.location.reload()
    }
}

/**
 * @param url url to upload a file
 * @param file a file which needs to be uploaded
 * @param loadingHandler
 * @return response of operation
 */
suspend fun WithRequestStatusContext.postUploadFile(
    url: String,
    file: File,
    loadingHandler: suspend (suspend () -> Response) -> Response,
): Response = post(
    url,
    Headers().apply { append(CONTENT_LENGTH_CUSTOM, file.size.toString()) },
    FormData().apply { set(FILE_PART_NAME, file) },
    loadingHandler = loadingHandler,
)

/**
 * Fetch debug info for test execution
 *
 * @param testExecutionDto
 * @return Response
 */
@Suppress("TYPE_ALIAS")
suspend fun ComponentWithScope<*, *>.getDebugInfoFor(
    testExecutionDto: TestExecutionDto,
) = getDebugInfoFor(testExecutionDto, this::get)

/**
 * Fetch debug info for test execution
 *
 * @param testExecutionDto
 * @return Response
 */
suspend fun WithRequestStatusContext.getDebugInfoFor(
    testExecutionDto: TestExecutionDto,
) = getDebugInfoFor(testExecutionDto, this::get)

/**
 * Fetch execution info for test execution
 *
 * @param testExecutionDto
 * @return Response
 */
@Suppress("TYPE_ALIAS")
suspend fun ComponentWithScope<*, *>.getExecutionInfoFor(
    testExecutionDto: TestExecutionDto,
) = get(
    "$apiUrl/files/get-execution-info",
    params = jso<dynamic> { executionId = testExecutionDto.executionId },
    jsonHeaders,
    ::noopLoadingHandler,
    ::noopResponseHandler
)

@Suppress("TYPE_ALIAS")
private suspend fun getDebugInfoFor(
    testExecutionDto: TestExecutionDto,
    get: suspend (String, dynamic, Headers, suspend (suspend () -> Response) -> Response, (Response) -> Unit) -> Response,
) = get(
    "$apiUrl/files/get-debug-info",
    jso { testExecutionId = testExecutionDto.requiredId() },
    jsonHeaders,
    ::noopLoadingHandler,
    ::noopResponseHandler,
)
