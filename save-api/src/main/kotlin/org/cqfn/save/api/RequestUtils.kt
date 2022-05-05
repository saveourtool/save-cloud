/**
 * Utilities, which extends http client functionality and provide api for execution submission process
 */

package org.cqfn.save.api

import org.cqfn.save.domain.FileInfo
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestBase
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.utils.LocalDateTimeSerializer
import org.cqfn.save.utils.extractUserNameAndSource
import org.cqfn.save.v1

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.json.*
import io.ktor.client.plugins.kotlinx.serializer.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import okio.Path.Companion.toPath

import java.io.File
import java.time.LocalDateTime

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

private val json = Json {
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}

private object Backend {
    lateinit var url: String
}

/**
 * @property username
 * @property source source (where the user identity is coming from)
 */
private object UserInformation {
    lateinit var username: String
    lateinit var source: String
}

/**
 * @param name
 * @return Organization instance
 */
suspend fun HttpClient.getOrganizationByName(
    name: String
): Organization = getRequestWithAuthAndJsonContentType(
    "${Backend.url}/api/$v1/organization/$name"
).body()

/**
 * @param projectName
 * @param organizationName
 * @return Project instance
 */
suspend fun HttpClient.getProjectByNameAndOrganizationName(
    projectName: String, organizationName: String
): Project = getRequestWithAuthAndJsonContentType(
    "${Backend.url}/api/$v1/projects/get/organization-name?name=$projectName&organizationName=$organizationName"
).body()

/**
 * @return list of available files from storage
 */
suspend fun HttpClient.getAvailableFilesList(
): List<FileInfo> = getRequestWithAuthAndJsonContentType(
    "${Backend.url}/api/$v1/files/list"
).body()

/**
 * @param file
 * @return FileInfo of uploaded file
 */
@OptIn(InternalAPI::class)
suspend fun HttpClient.uploadAdditionalFile(
    file: String,
): FileInfo = this.post {
    url("${Backend.url}/api/$v1/files/upload")
    header("X-Authorization-Source", UserInformation.source)
    body = MultiPartFormDataContent(formData {
        append(
            key = "file",
            value = File(file).readBytes(),
            headers = Headers.build {
                append(HttpHeaders.ContentDisposition, "filename=${file.toPath().name}")
            }
        )
    })
}.body()

/**
 * @return list of existing standard test suites
 */
suspend fun HttpClient.getStandardTestSuites(
): List<TestSuiteDto> = getRequestWithAuthAndJsonContentType(
    "${Backend.url}/api/$v1/allStandardTestSuites"
).body()

/**
 * Submit execution, according [executionType] with list of [additionalFiles]
 *
 * @param executionType type of requested execution git/standard
 * @param executionRequest execution request
 * @param additionalFiles list of additional files for execution
 * @return HttpResponse
 */
@OptIn(InternalAPI::class)
@Suppress("TOO_LONG_FUNCTION")
suspend fun HttpClient.submitExecution(executionType: ExecutionType, executionRequest: ExecutionRequestBase, additionalFiles: List<FileInfo>?): HttpResponse {
    val endpoint = if (executionType == ExecutionType.GIT) {
        "/api/$v1/submitExecutionRequest"
    } else {
        "/api/$v1/executionRequestStandardTests"
    }
    return this.post {
        url("${Backend.url}$endpoint")
        header("X-Authorization-Source", UserInformation.source)
        val formDataHeaders = Headers.build {
            append(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        setBody(MultiPartFormDataContent(formData {
            if (executionType == ExecutionType.GIT) {
                append(
                    "executionRequest",
                    json.encodeToString(executionRequest as ExecutionRequest),
                    formDataHeaders
                )
            } else {
                append(
                    "execution",
                    json.encodeToString(executionRequest as ExecutionRequestForStandardSuites),
                    formDataHeaders
                )
            }
            additionalFiles?.forEach { fileInfo ->
                append(
                    "file",
                    json.encodeToString(fileInfo),
                    Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                )
            }
        }))
    }
}

/**
 * @param projectName
 * @param organizationId
 * @return ExecutionDto
 */
suspend fun HttpClient.getLatestExecution(
    projectName: String,
    organizationName: String
): ExecutionDto = getRequestWithAuthAndJsonContentType(
    "${Backend.url}/api/$v1/latestExecution?name=$projectName&organizationName=$organizationName"
).body()

/**
 * @param executionId
 * @return ExecutionDto
 */
suspend fun HttpClient.getExecutionById(
    executionId: Long
): ExecutionDto = getRequestWithAuthAndJsonContentType(
    "${Backend.url}/api/$v1/executionDto?executionId=$executionId"
).body()

private suspend fun HttpClient.getRequestWithAuthAndJsonContentType(url: String): HttpResponse = this.get {
    url(url)
    header("X-Authorization-Source", UserInformation.source)
    contentType(ContentType.Application.Json)
}

/**
 * @param authorization authorization settings
 * @param webClientProperties http client configuration
 * @return HttpClient instance
 */
fun initializeHttpClient(
    authorization: Authorization,
    webClientProperties: WebClientProperties,
): HttpClient {
    Backend.url = webClientProperties.backendUrl
    val (name, source) = extractUserNameAndSource(authorization.userInformation)
    UserInformation.username = name
    UserInformation.source = source

    return HttpClient(Apache) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.NONE
        }
        install(JsonPlugin) {
            serializer = KotlinxSerializer(json)
        }
        install(Auth) {
            basic {
                // by default, ktor will wait for the server to respond with 401,
                // and only then send the authentication header
                // therefore, adding sendWithoutRequest is required
                sendWithoutRequest { true }
                credentials {
                    BasicAuthCredentials(username = authorization.userInformation, password = authorization.token ?: "")
                }
            }
        }
    }
}
