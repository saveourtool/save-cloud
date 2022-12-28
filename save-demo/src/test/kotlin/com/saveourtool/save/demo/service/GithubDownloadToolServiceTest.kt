package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.entity.GithubRepo
import com.saveourtool.save.demo.storage.ToolStorage
import com.saveourtool.save.demo.storage.toToolKey
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component

@Import(
    ToolStorage::class,
    ToolService::class,
)

@Component
class GithubDownloadToolServiceTest
{
    @Autowired private lateinit var githubDownloadToolService: GithubDownloadToolService
    @Autowired private lateinit var toolStorage: ToolStorage
    @Autowired private lateinit var toolService: ToolService
    @Suppress("InjectDispatcher")
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    private val githubDownloadToolServiceForTest : GithubDownloadToolService by lazy {
        GithubDownloadToolService(
            toolStorage = toolStorage,
            toolService = toolService,
            httpClient = HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.encodedPath) {
                            CONNECT_KTLINT -> respond ("ktlintData", HttpStatusCode.OK,)
                            CONNECT_DIKTAT -> respond ("diktatData", HttpStatusCode.OK,)
                            else -> error("Unhandled ${request.url}")
                        }
                    }
                }
            },
        )
    }


    @Test
    fun `check the connection to the server to search for the version`() {
        runBlocking {
            githubDownloadToolServiceForTest.run {
                sequenceOf(
                    DiktatDemoTool.KTLINT.toToolKey(CONNECT_KTLINT),
                    DiktatDemoTool.DIKTAT.toToolKey(CONNECT_DIKTAT)
                ).forEach { key ->
                    getMetadata (GithubRepo(key.ownerName, key.toolName), key.vcsTagName,)
                }
            }
        }
    }


    @Test
    fun `checking the connection to the server to upload data`() {
        runBlocking {
            githubDownloadToolServiceForTest.run {
                sequenceOf(
                    DiktatDemoTool.KTLINT.toToolKey(CONNECT_KTLINT),
                    DiktatDemoTool.DIKTAT.toToolKey(CONNECT_DIKTAT)
                ).forEach { key ->
                    scope.launch {
                        downloadAsset(getExecutable(GithubRepo(key.ownerName, key.toolName), key.vcsTagName,))
                    }
                }
            }
        }
    }

    companion object {
        private const val CONNECT_KTLINT = "ktlint.jar"
        private const val CONNECT_DIKTAT = "diktat-1.2.3.jar"
    }
}