package org.cqfn.save.api

import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Pass
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.utils.LocalDateTimeSerializer

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.util.InternalAPI
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory

import java.io.IOException
import java.time.LocalDateTime
import java.util.Properties

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.cqfn.save.domain.Jdk
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectStatus

internal val json = Json {
    serializersModule = SerializersModule {
        polymorphic(TestResultDebugInfo::class)
        polymorphic(DebugInfo::class)
        polymorphic(Pass::class)
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}

class AutomaticTestInitializator {
    private val log = LoggerFactory.getLogger(AutomaticTestInitializator::class.java)
    private val httpClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
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

    @OptIn(InternalAPI::class)
    suspend fun start() {
        val webClientProperties = readWebClientProperties()
        requireNotNull(webClientProperties) {
            "Configuration couldn't be empty!"
        }

        submitExecution(webClientProperties)
    }

    suspend fun getStandardTestSuites(webClientProperties: WebClientProperties) {
        log.info("\n\n-------------------Get all standard test suites---------------------")

        val response = httpClient.get<HttpResponse> {
            url("${webClientProperties.backendUrl}/api/allStandardTestSuites")
            header("X-Authorization-Source", "basic")
            contentType(ContentType.Application.Json)
        }
        log.info("\n\n\n==========================\nStatus: ${response.status}\n")

        val result = response.receive<List<TestSuiteDto>>()
        log.info("Result: $result")
    }

    @OptIn(InternalAPI::class)
    suspend fun submitExecution(webClientProperties: WebClientProperties) {
        log.info("\n\n-------------------Start execution---------------------")
        val organizationName = "Huawei"

        val organization = getOrganizationByName(webClientProperties, organizationName)
        val organizationId = organization.id
        val userId = organization.ownerId

        println("ORG ${organizationId} owner ${userId}")

        return

//        val organization = Organization(
//            name = organizationName,
//            ownerId = userId,
//            dateCreated = LocalDateTime.parse("2021-01-01T00:00:00"),
//            avatar = null,
//        ).apply {
//            id = organizationId
//        }

        val gitUrl = "https://github.com/analysis-dev/save-cli"
        val projectId = 5L

        val project = Project(
            name = "save",
            url = gitUrl,
            description = "description",
            status = ProjectStatus.CREATED,
            public = true,
            userId = userId,
            organization = organization,
        ).apply {
            id = projectId
        }


        val userName = "admin"
        val branch = "origin/feature/testing_for_cloud"

        val gitDto = GitDto(
            url = gitUrl,
            username = userName,
            password = null,
            branch = branch,
            hash = null
        )

        val testRootPath = "examples/kotlin-diktat"
        val executionId = 4L

        val executionRequest = ExecutionRequest(
            project = project,
            gitDto = gitDto,
            testRootPath = testRootPath,
            sdk = Jdk("11"),
            executionId = executionId,
        )

        httpClient.post<HttpResponse> {
            url("${webClientProperties.backendUrl}/api/submitExecutionRequest")
            header("X-Authorization-Source", "basic")
            body = MultiPartFormDataContent(formData {
                //contentType(ContentType.Application.Json)
                //append("Jonh", "Doe")

                append("executionRequest", json.encodeToString(executionRequest),
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                    }
                )


                // TODO provide logic for files
                // append("file", "")
            })
        }

    }


    suspend fun getOrganizationByName(webClientProperties: WebClientProperties, name: String): Organization {
        return httpClient.get<HttpResponse> {
            url("${webClientProperties.backendUrl}/api/organization/get/organization-name?name=${name}")
            header("X-Authorization-Source", "basic")
            contentType(ContentType.Application.Json)
        }.receive()
    }

    private fun readWebClientProperties(configFileName: String = "web-client.properties"): WebClientProperties? {
        try {
            val properties = Properties()
            val classLoader = AutomaticTestInitializator::class.java.classLoader
            val input = classLoader.getResourceAsStream(configFileName)
            if (input == null) {
                log.error("Unable to find configuration file: $configFileName")
                return null
            }
            properties.load(input)
            return WebClientProperties(
                properties.getProperty("backendUrl"),
                properties.getProperty("preprocessorUrl"),
            )
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
    }

    /**
     * @property backendUrl
     * @property preprocessorUrl
     */
    data class WebClientProperties(
        val backendUrl: String,
        val preprocessorUrl: String,
    )
}

//
//        val testExecutionDto = TestExecutionDto(
//            filePath = "stub",
//            pluginName = "stub",
//            agentContainerId = "stub",
//            status = TestResultStatus.PASSED,
//            startTimeSeconds = LocalDateTime.parse("2021-01-01 00:03:07.000").toEpochSecond(ZoneOffset.UTC),
//            endTimeSeconds = LocalDateTime.parse("2021-01-01 00:03:44.000").toEpochSecond(ZoneOffset.UTC),
//            testSuiteName= null,
//            tags= emptyList(),
//            missingWarnings = 0,
//            matchedWarnings = 0,
//        )
