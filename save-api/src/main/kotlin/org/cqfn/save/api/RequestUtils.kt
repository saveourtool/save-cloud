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

/**
 * @property webClientProperties
 */
class RequestUtils(
    private val webClientProperties: WebClientProperties,
) {
    private val httpClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
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
    suspend fun submitExecution(executionRequest: ExecutionRequest, additionalFiles: List<FileInfo>?) {
        httpClient.post<HttpResponse> {
            url("${webClientProperties.backendUrl}/api/submitExecutionRequest")
            header("X-Authorization-Source", "basic")
            body = MultiPartFormDataContent(formData {
                append("executionRequest", json.encodeToString(executionRequest),
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                )
                additionalFiles?.forEach {
                    append("file", json.encodeToString(it), Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    })
                }
            })
        }
    }

    private suspend fun getRequestWithAuthAndJsonContentType(url: String): HttpResponse = httpClient.get {
        url(url)
        header("X-Authorization-Source", "basic")
        contentType(ContentType.Application.Json)
    }
}
