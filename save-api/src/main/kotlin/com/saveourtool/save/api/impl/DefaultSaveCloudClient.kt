@file:Suppress("TYPE_ALIAS")

package com.saveourtool.save.api.impl

import com.saveourtool.save.api.SaveCloudClientEx
import com.saveourtool.save.api.errors.SaveCloudError
import com.saveourtool.save.api.errors.TimeoutError
import com.saveourtool.save.api.http.deleteAndCheck
import com.saveourtool.save.api.http.getAndCheck
import com.saveourtool.save.api.http.postAndCheck
import com.saveourtool.save.api.io.readChannel
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.entities.*
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.filters.ProjectFilters
import com.saveourtool.save.request.CreateExecutionRequest
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.supportJLocalDateTime
import com.saveourtool.save.v1

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application
import io.ktor.http.ContentType.Text
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders.ContentDisposition
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.contentType
import io.ktor.http.escapeIfNeeded
import io.ktor.serialization.kotlinx.json.json

import java.lang.System.nanoTime
import java.net.URL
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

import kotlin.coroutines.CoroutineContext
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.system.measureNanoTime
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * _SAVE_ REST API client, the default implementation.
 *
 * @param backendUrl the URL of a _SAVE_ backend (e.g.:
 *   `http://localhost:5800`) or a _SAVE_ gateway (e.g.:
 *   `http://localhost:5300`).
 * @param ioContext the context to be used for I/O.
 * @param requestTimeoutMillis HTTP request timeout, in milliseconds.
 *   Should be large enough, otherwise the process of uploading large
 *   files may fail.
 * @param socketTimeoutMillis TCP socket timeout, in milliseconds.
 * @param authConfiguration authentication configuration.
 */
internal class DefaultSaveCloudClient(
    private val backendUrl: URL,
    private val ioContext: CoroutineContext,
    requestTimeoutMillis: Long,
    socketTimeoutMillis: Long,
    authConfiguration: Auth.() -> Unit
) : SaveCloudClientEx {
    private val httpClient = httpClient(
        requestTimeoutMillis,
        socketTimeoutMillis,
        authConfiguration
    )

    override suspend fun listOrganizations(): Either<SaveCloudError, List<Organization>> =
            getAndCheck("/organizations/get/list")

    override suspend fun listProjects(organizationName: String): Either<SaveCloudError, List<Project>> =
            postAndCheck(
                "/projects/by-filters",
                requestBody = Json.encodeToString(ProjectFilters("", ORGANIZATION_NAME, ProjectStatus.values().toSet())),
            )

    override suspend fun listTestSuites(organizationName: String): Either<SaveCloudError, List<TestSuiteDto>> =
            getAndCheck("/test-suites/$organizationName/available")

    override suspend fun listFiles(
        organizationName: String,
        projectName: String
    ): Either<SaveCloudError, List<FileInfo>> =
            getAndCheck("/files/$organizationName/$projectName/list")

    override suspend fun uploadFile(
        organizationName: String,
        projectName: String,
        file: Path,
        contentType: ContentType?,
        stripVersionFromName: Boolean
    ): Either<SaveCloudError, FileInfo> {
        val absoluteFile = file.toAbsolutePath().normalize()

        require(absoluteFile.isRegularFile()) {
            when {
                absoluteFile.isDirectory() -> "$absoluteFile is a directory"
                absoluteFile.exists() -> "$absoluteFile is not a regular file"
                else -> "$absoluteFile doesn't exist"
            }
        }

        val fileInfo: FileInfo
        val nanos = measureNanoTime {
            fileInfo = postAndCheck<FileInfo>(
                "/files/$organizationName/$projectName/upload",
                MultiPartFormDataContent(formData {
                    val fileName = absoluteFile.fileName.toString().let { fileName ->
                        when {
                            stripVersionFromName -> fileName.stripVersion()
                            else -> fileName
                        }
                    }

                    val headers = Headers.build {
                        this[ContentDisposition] = "filename=${fileName.escapeIfNeeded()}"
                        contentType?.let {
                            this[ContentType] = contentType.toString()
                        }
                    }

                    append(
                        key = "file",
                        value = ChannelProvider {
                            absoluteFile.readChannel(ioContext = ioContext)
                        },
                        headers
                    )
                })
            ).getOrHandle { error ->
                return error.left()
            }
        }
        @Suppress("MagicNumber", "FLOAT_IN_ACCURATE_CALCULATIONS")
        logger.debug("Uploaded ${absoluteFile.fileSize()} byte(s) in ${nanos / 1000L / 1e3} ms.")

        return fileInfo.right()
    }

    override suspend fun listExecutions(
        organizationName: String,
        projectName: String
    ): Either<SaveCloudError, List<ExecutionDto>> =
            getAndCheck(
                "/executionDtoList",
                requestBody = EmptyContent,
                ORGANIZATION_NAME to organizationName,
                NAME to projectName
            )

    override suspend fun submitExecution(
        request: CreateExecutionRequest,
        timeoutValue: Long,
        timeoutUnit: TimeUnit
    ): Either<SaveCloudError, ExecutionDto> {
        val (organizationName, projectName) = request.projectCoordinates

        val ignoredExecutionIds = listExecutions(organizationName, projectName).getOrHandle { error ->
            return error.left()
        }
            .asSequence()
            .map(ExecutionDto::id)
            .toSet()

        return postAndCheck<Unit>("/run/trigger", request, Application.Json).flatMap {
            repeatUntilResultAvailable(
                timeoutValue,
                timeoutUnit,
                "while waiting for the submitted execution to appear"
            ) {
                listExecutions(organizationName, projectName).map { executions ->
                    executions.firstOrNull { (id) ->
                        id !in ignoredExecutionIds
                    }
                }
            }
        }
    }

    override suspend fun getExecutionById(id: Long): Either<SaveCloudError, ExecutionDto> =
            getAndCheck(
                "/executionDto",
                requestBody = EmptyContent,
                EXECUTION_ID to id
            )

    override suspend fun deleteFile(
        organizationName: String,
        projectName: String,
        fileName: String,
        fileTimestamp: Long
    ): Either<SaveCloudError, Unit> =
            deleteAndCheck(
                "/files/$organizationName/$projectName/delete",
                requestBody = EmptyContent,
                accept = Text.Plain,
                NAME to fileName,
                UPLOADED_MILLIS to fileTimestamp
            )

    override suspend fun listActiveContests(
        organizationName: String,
        projectName: String,
        limit: Int
    ): Either<SaveCloudError, List<ContestDto>> =
            getAndCheck<List<ContestResult>>(
                "/contests/$organizationName/$projectName/best",
                requestBody = EmptyContent,
                AMOUNT to limit,
            ).map { contestResults ->
                contestResults
                    .asSequence()
                    .map(ContestResult::contestName)
                    .toSet()
            }.flatMap { contestNames ->
                getAndCheck<List<ContestDto>>(
                    "/contests/active",
                ).map { contests ->
                    /*
                     * Leave only those contests which the given project has
                     * enrolled in.
                     */
                    contests
                        .asSequence()
                        .filter { (name) ->
                            name in contestNames
                        }.toList()
                }
            }

    private suspend inline fun <reified T> getAndCheck(
        absolutePath: String,
        requestBody: Any = EmptyContent,
        vararg parameters: Pair<String, Any?>
    ): Either<SaveCloudError, T> =
            httpClient.getAndCheck {
                url(backendUrl.apiUrl(absolutePath))
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
                accept(Application.Json)
                setBody(requestBody)
            }

    private suspend inline fun <reified T> postAndCheck(
        absolutePath: String,
        requestBody: Any = EmptyContent,
        contentType: ContentType? = null,
        vararg parameters: Pair<String, Any?>
    ): Either<SaveCloudError, T> =
            httpClient.postAndCheck {
                url(backendUrl.apiUrl(absolutePath))
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
                accept(Application.Json)
                contentType?.let {
                    contentType(contentType)
                }
                setBody(requestBody)
            }

    private suspend inline fun <reified T> deleteAndCheck(
        absolutePath: String,
        requestBody: Any = EmptyContent,
        accept: ContentType = Application.Json,
        vararg parameters: Pair<String, Any?>
    ): Either<SaveCloudError, T> =
            httpClient.deleteAndCheck {
                url(backendUrl.apiUrl(absolutePath))
                parameters.forEach { (key, value) ->
                    parameter(key, value)
                }
                accept(accept)
                setBody(requestBody)
            }

    private companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<DefaultSaveCloudClient>()
        private const val AMOUNT = "amount"
        private const val DEFAULT_API_VERSION = v1
        private const val EXECUTION_ID = "executionId"
        private const val NAME = "name"
        private const val ORGANIZATION_NAME = "organizationName"
        private const val POLL_DELAY_MILLIS = 100L
        private const val UPLOADED_MILLIS = "uploadedMillis"
        private val fileWithVersion =
                Regex("""^(?<basename>.+?)-(?<version>\d+(?:\.\d+)*)(?<extension>(?:\.[^.\s-]+)+?)?$""")

        private fun httpClient(
            requestTimeoutMillis: Long,
            socketTimeoutMillis: Long,
            authConfiguration: Auth.() -> Unit
        ): HttpClient =
                HttpClient {
                    install(HttpTimeout) {
                        this.requestTimeoutMillis = requestTimeoutMillis
                        this.socketTimeoutMillis = socketTimeoutMillis
                    }
                    install(Auth, authConfiguration)
                    install(ContentNegotiation) {
                        val json = Json {
                            serializersModule = SerializersModule {
                                supportJLocalDateTime()
                            }
                        }

                        json(json)
                    }
                }

        private fun URL.apiUrl(absolutePath: String, version: String = DEFAULT_API_VERSION): URL {
            require(absolutePath.startsWith('/')) {
                absolutePath
            }

            return "$this/api/$version$absolutePath".let(::URL)
        }

        private fun String.stripVersion(): String {
            val matchResult = fileWithVersion.matchEntire(this) ?: return this

            val basename = matchResult.groups["basename"]?.value ?: return this

            val version = matchResult.groups["version"]?.value
            logger.debug("Stripping version \"$version\" from $this")

            /*
             * The extension with a leading dot, e.g.: `.tar.gz` or `.Z`.
             */
            val extension = matchResult.groups["extension"]?.value.orEmpty()

            return basename + extension
        }

        @Suppress("NESTED_BLOCK")
        private suspend fun <T : Any> repeatUntilResultAvailable(
            timeoutValue: Long,
            timeoutUnit: TimeUnit = MILLISECONDS,
            errorMessageSuffix: String = "",
            block: suspend () -> Either<SaveCloudError, T?>
        ): Either<SaveCloudError, T> {
            val startNanos = nanoTime()

            while (true) {
                val result = block().getOrHandle { error ->
                    /*
                     * Return immediately if an error has been encountered.
                     */
                    return error.left()
                }

                return when (result) {
                    null -> {
                        val elapsedNanos = nanoTime() - startNanos

                        when {
                            /*
                             * Time-out.
                             */
                            elapsedNanos > timeoutUnit.toNanos(timeoutValue) -> TimeoutError(
                                timeoutValue,
                                timeoutUnit,
                                errorMessageSuffix
                            ).left()

                            else -> {
                                delay(POLL_DELAY_MILLIS)
                                continue
                            }
                        }
                    }

                    else -> result.right()
                }
            }
        }
    }
}
