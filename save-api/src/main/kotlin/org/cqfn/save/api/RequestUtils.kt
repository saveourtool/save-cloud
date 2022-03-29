package org.cqfn.save.api

import org.cqfn.save.domain.FileInfo
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.testsuite.TestSuiteDto

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import okio.Path.Companion.toPath

import java.io.File

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.cqfn.save.entities.ExecutionRequestBase
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.utils.LocalDateTimeSerializer
import java.time.LocalDateTime

private val json = Json {
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}

/**
 * @property webClientProperties
 */
class RequestUtils(
    private val webClientProperties: WebClientProperties,
) {
    private val httpClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.NONE
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
        install(Auth) {
            basic {
                // by default, ktor will wait for the server to respond with 401,
                // and only then send the authentication header
                // therefore, adding sendWithoutRequest is required
                sendWithoutRequest { true }
                credentials {
                    // TODO: pass via configuration
                    BasicAuthCredentials(username = "admin", password = "")
                }
            }
        }
    }

    /**
     * @param name
     * @return organization instance
     */
    suspend fun getOrganizationByName(
        name: String
    ): Organization = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/organization/get/organization-name?name=$name"
    ).receive()

    /**
     * @param projectName
     * @param organizationId
     * @return Project instance
     */
    suspend fun getProjectByNameAndOrganizationId(
        projectName: String, organizationId: Long
    ): Project = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/projects/get/organization-id?name=$projectName&organizationId=$organizationId"
    ).receive()

    /**
     * @return list of available files from storage
     */
    suspend fun getAvailableFilesList(
    ): List<FileInfo> = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/files/list"
    ).receive()

    /**
     * @param file
     * @return FileInfo instance
     */
    @OptIn(InternalAPI::class)
    suspend fun uploadAdditionalFile(
        file: String,
    ): FileInfo = httpClient.post {
        url("${webClientProperties.backendUrl}/api/files/upload")
        header("X-Authorization-Source", "basic")
        body = MultiPartFormDataContent(formData {
            append(
                key = "file",
                value = File(file).readBytes(),
                headers = Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=${file.toPath().name}")
                }
            )
        })
    }

    /**
     * @return
     */
    suspend fun getStandardTestSuites(
    ): List<TestSuiteDto> = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/allStandardTestSuites"
    ).receive()

    /**
     * @param executionRequest
     * @param additionalFiles
     */
    @OptIn(InternalAPI::class)
    suspend fun submitExecution(executionType: ExecutionType, executionRequest: ExecutionRequestBase, additionalFiles: List<FileInfo>?) {
        val endpoint = if (executionType == ExecutionType.GIT) {
            "/api/submitExecutionRequest"
        } else {
            "/api/executionRequestStandardTests"
        }
        httpClient.post<HttpResponse> {
            url("${webClientProperties.backendUrl}${endpoint}")
            header("X-Authorization-Source", "basic")
            val formDataHeaders = Headers.build {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
            }
            body = MultiPartFormDataContent(formData {
                if (executionType == ExecutionType.GIT) {
                    append("executionRequest", json.encodeToString(executionRequest as ExecutionRequest),
                        formDataHeaders
                    )
                } else {
                    append("execution", json.encodeToString(executionRequest as ExecutionRequestForStandardSuites),
                        formDataHeaders
                    )
                }

                additionalFiles?.forEach {
                    append("file", json.encodeToString(it), Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    })
                }
            })
        }
    }

    suspend fun getLatestExecution(
        projectName: String,
        organizationId: Long
    ): ExecutionDto = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/latestExecution?name=${projectName}&organizationId=${organizationId}"
    ).receive()

    suspend fun getExecutionById(
        executionId: Long
    ): ExecutionDto = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/executionDto?executionId=${executionId}"
    ).receive()

    private suspend fun getRequestWithAuthAndJsonContentType(url: String): HttpResponse = httpClient.get {
        url(url)
        header("X-Authorization-Source", "basic")
        contentType(ContentType.Application.Json)
    }
}
