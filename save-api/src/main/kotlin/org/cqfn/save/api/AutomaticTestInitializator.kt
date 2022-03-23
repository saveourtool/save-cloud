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
        val webClientProperties = readPropertiesFile(obj = ConfigurationType.WEB_CLIENT)
        requireNotNull(webClientProperties) {
            "Configuration couldn't be empty!"
        }

        submitExecution(webClientProperties as WebClientProperties)
    }

    @OptIn(InternalAPI::class)
    suspend fun submitExecution(webClientProperties: WebClientProperties) {
        log.info("\n\n-------------------Start execution---------------------")
        val organizationName = "Huawei"
        val projectName = "save"

        val gitUrl = "https://github.com/analysis-dev/save-cli"
        val gitUserName = "admin"
        val gitPassword = null
        val branch = "origin/feature/testing_for_cloud"
        val commitHash = null

        val testRootPath = "examples/kotlin-diktat"

        val organization = getOrganizationByName(webClientProperties, organizationName)
        val organizationId = organization.id!!
        val project = getProjectByNameAndOrganizationId(webClientProperties, projectName, organizationId)
        val gitDto = GitDto(
            url = gitUrl,
            username = gitUserName,
            password = gitPassword,
            branch = branch,
            hash = commitHash
        )

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

    suspend fun submitExecutionStandardMode() {
        TODO("Not yet implemented")
    }


    private suspend fun getOrganizationByName(webClientProperties: WebClientProperties, name: String): Organization {
        return httpClient.get<HttpResponse> {
            url("${webClientProperties.backendUrl}/api/organization/get/organization-name?name=${name}")
            header("X-Authorization-Source", "basic")
            contentType(ContentType.Application.Json)
        }.receive()
    }

    private suspend fun getProjectByNameAndOrganizationId(webClientProperties: WebClientProperties, projectName: String, organizationId: Long): Project {
        return httpClient.get<HttpResponse> {
            url("${webClientProperties.backendUrl}/api/projects/get/organization-id?name=${projectName}&organizationId=${organizationId}")
            header("X-Authorization-Source", "basic")
            contentType(ContentType.Application.Json)
        }.receive()
    }

    private suspend fun getStandardTestSuites(webClientProperties: WebClientProperties): List<TestSuiteDto> {
        return httpClient.get<HttpResponse> {
            url("${webClientProperties.backendUrl}/api/allStandardTestSuites")
            header("X-Authorization-Source", "basic")
            contentType(ContentType.Application.Json)
        }.receive()
    }

    private fun readPropertiesFile(configFileName: String = "web-client.properties", obj: ConfigurationType): Configuration? {
        try {
            val properties = Properties()
            val classLoader = AutomaticTestInitializator::class.java.classLoader
            val input = classLoader.getResourceAsStream(configFileName)
            if (input == null) {
                log.error("Unable to find configuration file: $configFileName")
                return null
            }
            properties.load(input)
            when(obj) {
                ConfigurationType.WEB_CLIENT -> return WebClientProperties(
                    properties.getProperty("backendUrl"),
                    properties.getProperty("preprocessorUrl"),
                )
                ConfigurationType.EVALUATED_TOOL -> return EvaluatedToolProperties(
                    properties.getProperty("organizationName"),
                    properties.getProperty("projectName"),
                    properties.getProperty("gitUrl"),
                    properties.getProperty("gitUserName"),
                    properties.getProperty("gitPassword"),
                    properties.getProperty("branch"),
                    properties.getProperty("commitHash"),
                    properties.getProperty("testRootPath"),
                )
                else -> {
                    // fixme:
                    log.error("Unsupported type for configuration")
                    return null
                }
            }

        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
    }

    enum class ConfigurationType {
        WEB_CLIENT,
        EVALUATED_TOOL,
        ;
    }


    open class Configuration



    /**
     * @property backendUrl
     * @property preprocessorUrl
     */
    data class WebClientProperties(
        val backendUrl: String,
        val preprocessorUrl: String,
    ): Configuration()

    data class EvaluatedToolProperties(
        val organizationName: String,
        val projectName: String,
        val gitUrl: String,
        val gitUserName: String,
        val gitPassword: String? = null,
        val branch: String? = null,
        val commitHash: String?,
        val testRootPath: String,
    ): Configuration()
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
