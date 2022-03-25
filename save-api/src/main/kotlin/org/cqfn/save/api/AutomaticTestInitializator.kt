package org.cqfn.save.api

import org.cqfn.save.domain.Jdk
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
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
import org.slf4j.LoggerFactory

import java.io.IOException
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.Properties

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.cqfn.save.domain.FileInfo
import java.io.File

internal val json = Json {
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}

class AutomaticTestInitializator {
    private val log = LoggerFactory.getLogger(AutomaticTestInitializator::class.java)
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
    private val webClientPropertiesFileName = "web-client.properties"
    private val evaluatedToolPropertiesFileName = "evaluated-tool.properties"

    /**
     * @throws IllegalArgumentException
     */
    @OptIn(InternalAPI::class)
    suspend fun start() {
        val webClientProperties = readPropertiesFile(webClientPropertiesFileName, PropertiesConfigurationType.WEB_CLIENT) as WebClientProperties?
        val evaluatedToolProperties = readPropertiesFile(evaluatedToolPropertiesFileName, PropertiesConfigurationType.EVALUATED_TOOL) as EvaluatedToolProperties?

        if (webClientProperties == null || evaluatedToolProperties == null) {
            throw IllegalArgumentException(
                "Configuration for web client and for evaluate tool couldn't be empty!" +
                        " Please make sure, that you have proper configuration in files: $webClientPropertiesFileName, $evaluatedToolPropertiesFileName"
            )
        }

        evaluatedToolProperties.additionalFiles?.let {
            processAdditionalFiles(webClientProperties, it)
        }

        submitExecution(webClientProperties, evaluatedToolProperties)
    }

    /**
     * @param webClientProperties
     * @param evaluatedToolProperties
     */
    @OptIn(InternalAPI::class)
    suspend fun submitExecution(webClientProperties: WebClientProperties, evaluatedToolProperties: EvaluatedToolProperties) {
        log.info("Starting submit execution")

        val organization = getOrganizationByName(webClientProperties, evaluatedToolProperties.organizationName)
        val organizationId = organization.id!!
        val project = getProjectByNameAndOrganizationId(webClientProperties, evaluatedToolProperties.projectName, organizationId)

        val gitDto = GitDto(
            url = evaluatedToolProperties.gitUrl,
            username = evaluatedToolProperties.gitUserName,
            password = evaluatedToolProperties.gitPassword,
            branch = evaluatedToolProperties.branch,
            hash = evaluatedToolProperties.commitHash
        )

        // Actually it's just a stub, executionId will be calculated at the server side
        val executionId = 1L

        val executionRequest = ExecutionRequest(
            project = project,
            gitDto = gitDto,
            testRootPath = evaluatedToolProperties.testRootPath,
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

    private suspend fun processAdditionalFiles(webClientProperties: WebClientProperties, files: String) {
        val additionalFiles = files.split(";")

        additionalFiles.forEach {
            require(File(it).exists()) {
                "Couldn't find additional file $it"
            }
        }


        val availableFiles = listOf<FileInfo>()//getAvaliableFilesList(webClientProperties)

        availableFiles.forEach {
            println("----${it}")
        }

        val resultFiles: MutableList<String> = mutableListOf()

        additionalFiles.forEach { additionalFileName ->
            val fileFromStorage = availableFiles.firstOrNull { it.name == additionalFileName }
            fileFromStorage?.let {
                resultFiles.add(
                    // fixme what with separators
                    "${webClientProperties.fileStorage}/${fileFromStorage.uploadedMillis}/${fileFromStorage.name}"
                )
            } ?: {
                // upload files
            }
        }

        println("==========RESULT:  ${resultFiles}")


    }


    private suspend fun getOrganizationByName(
        webClientProperties: WebClientProperties,
        name: String
    ): Organization = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/organization/get/organization-name?name=$name"
    ).receive()

    private suspend fun getProjectByNameAndOrganizationId(
        webClientProperties: WebClientProperties,
        projectName: String, organizationId: Long
    ): Project = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/projects/get/organization-id?name=$projectName&organizationId=$organizationId"
    ).receive()

    private suspend fun getAvaliableFilesList(
        webClientProperties: WebClientProperties
    ): List<FileInfo> = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/files/list"
    ).receive()

    private suspend fun getStandardTestSuites(
        webClientProperties: WebClientProperties
    ): List<TestSuiteDto> = getRequestWithAuthAndJsonContentType(
        "${webClientProperties.backendUrl}/api/allStandardTestSuites"
    ).receive()

    private suspend fun getRequestWithAuthAndJsonContentType(url: String): HttpResponse = httpClient.get {
        url(url)
        header("X-Authorization-Source", "basic")
        contentType(ContentType.Application.Json)
    }

    private fun readPropertiesFile(configFileName: String, type: PropertiesConfigurationType): PropertiesConfiguration? {
        try {
            val properties = Properties()
            val classLoader = AutomaticTestInitializator::class.java.classLoader
            val input = classLoader.getResourceAsStream(configFileName)
            if (input == null) {
                log.error("Unable to find configuration file: $configFileName")
                return null
            }
            properties.load(input)
            when (type) {
                PropertiesConfigurationType.WEB_CLIENT -> return WebClientProperties(
                    properties.getProperty("backendUrl"),
                    properties.getProperty("preprocessorUrl"),
                    properties.getProperty("fileStorage"),
                )
                PropertiesConfigurationType.EVALUATED_TOOL -> return EvaluatedToolProperties(
                    properties.getProperty("organizationName"),
                    properties.getProperty("projectName"),
                    properties.getProperty("gitUrl"),
                    properties.getProperty("gitUserName"),
                    properties.getProperty("gitPassword"),
                    properties.getProperty("branch"),
                    properties.getProperty("commitHash"),
                    properties.getProperty("testRootPath"),
                    properties.getProperty("additionalFiles"),
                )
                else -> {
                    log.error("Type $type for properties configuration doesn't supported!")
                    return null
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
    }

    enum class PropertiesConfigurationType {
        EVALUATED_TOOL,
        WEB_CLIENT,
        ;
    }

    sealed class PropertiesConfiguration

    /**
     * @property backendUrl
     * @property preprocessorUrl
     */
    data class WebClientProperties(
        val backendUrl: String,
        val preprocessorUrl: String,
        val fileStorage: String,
    ) : PropertiesConfiguration()

    /**
     * @property organizationName
     * @property projectName
     * @property gitUrl
     * @property gitUserName
     * @property gitPassword
     * @property branch
     * @property commitHash
     * @property testRootPath
     */
    data class EvaluatedToolProperties(
        val organizationName: String,
        val projectName: String,
        val gitUrl: String,
        val gitUserName: String? = null,
        val gitPassword: String? = null,
        val branch: String? = null,
        val commitHash: String?,
        val testRootPath: String,
        val additionalFiles: String? = null,
    ) : PropertiesConfiguration()
}
