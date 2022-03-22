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
import io.ktor.http.contentType
import io.ktor.http.ContentType
import io.ktor.util.InternalAPI
import kotlinx.serialization.Contextual
import org.slf4j.LoggerFactory

import java.io.IOException
import java.time.LocalDateTime
import java.util.Properties

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.cqfn.save.domain.Jdk
import org.cqfn.save.domain.Sdk
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
        val configuration = readConfiguration()
        requireNotNull(configuration) {
            "Configuration couldn't be empty!"
        }

        log.info("\n\n-------------------Get all standard test suites---------------------")

        val response = httpClient.get<HttpResponse> {
            url("${configuration.backendUrl}/api/allStandardTestSuites")
            header("X-Authorization-Source", "basic")
            contentType(ContentType.Application.Json)
        }

        log.info("\n\n\n==========================\nStatus: ${response.status}\n")

        val result = response.receive<List<TestSuiteDto>>()
        log.info("Result: $result")

        log.info("-------------------Start execution---------------------")
        submitExecution(configuration)
    }

    @OptIn(InternalAPI::class)
    suspend fun submitExecution(configuration: ConfigProperties) {
        val userId = 42L
        val organizationId = 43L

        val organization = Organization(
            name = "test-organization",
            ownerId = userId,
            dateCreated = LocalDateTime.now(),
            avatar = "",
        ).apply {
            id = organizationId
        }

        val gitUrl = "https://github.com/analysis-dev/save-cli"
        val projectId = 44L

        val project = Project(
            name = "Test-project",
            url = gitUrl,
            description = "description",
            status = ProjectStatus.CREATED,
            public = true,
            userId = userId,
            organization = organization,
        ).apply {
            id = projectId
        }


        val userName = "user"
        val branch = "origin/feature/testing_for_cloud"

        val gitDto = GitDto(
            url = gitUrl,
            username = userName,
            password = null,
            branch = branch,
            hash = null
        )

        val testRootPath = "examples/kotlin-diktat"
        val executionId = 45L

        val executionRequest = ExecutionRequest(
            project = project,
            gitDto = gitDto,
            testRootPath = testRootPath,
            sdk = Jdk("11"),
            executionId = executionId,
        )

        httpClient.post<HttpResponse> {
            url("${configuration.backendUrl}/api/submitExecutionRequest")
            header("X-Authorization-Source", "basic")
            body = MultiPartFormDataContent(formData {
                append("executionRequest", executionRequest)
                // TODO provide logic for files
                // append("file", "")
            })
        }

    }


    private fun readConfiguration(configFileName: String = "config.properties"): ConfigProperties? {
        try {
            val properties = Properties()
            val classLoader = AutomaticTestInitializator::class.java.classLoader
            val input = classLoader.getResourceAsStream(configFileName)
            if (input == null) {
                log.error("Unable to find configuration file: $configFileName")
                return null
            }
            properties.load(input)
            return ConfigProperties(
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
    data class ConfigProperties(
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
