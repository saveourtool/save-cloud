package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entities.ReleaseAsset
import com.saveourtool.save.demo.entities.ReleaseMetadata
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.storage.ToolStorage
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.nio.ByteBuffer
import javax.annotation.PostConstruct

@Service
class GithubDownloadToolService(
    private val toolStorage: ToolStorage,
) {
    private val jsonSerializer = Json { ignoreUnknownKeys = true }
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val httpClient = httpClient()

    private fun getGithubMetadataUrlByKey(key: ToolKey) = with(key) {
        val release = if (version == LATEST_VERSION) { version } else { "tags/$version" }
        "$GITHUB_API_URL/$ownerName/$toolName/releases/$release"
    }

    private fun getMetadata(key: ToolKey): ReleaseMetadata {
        val channel = Channel<ReleaseMetadata>()
        scope.launch {
            httpClient.get(getGithubMetadataUrlByKey(key))
                .bodyAsText()
                .let {
                    jsonSerializer.decodeFromString<ReleaseMetadata>(it)
                }
                .let {
                    channel.send(it)
                    channel.close()
                }
        }
        return runBlocking { channel.receive() }
    }

    private fun downloadToolByUrl(url: String): Flux<ByteBuffer> {
        val byteChannel = ByteChannel()
        scope.launch {
            httpClient.get(url)
                .bodyAsChannel()
                .copyTo(byteChannel)
        }
        val listOfByteBuffers: MutableList<ByteBuffer> = mutableListOf()
        runBlocking {
            byteChannel.read {
                listOfByteBuffers.add(it)
            }
        }
        return listOfByteBuffers.toFlux()
    }

    private fun downloadFromGithub(key: ToolKey): Flux<ByteBuffer> = getMetadata(key).assets
        .filterNot(ReleaseAsset::isDigest)
        .first()
        .let { asset ->
            downloadToolByUrl(asset.downloadUrl)
        }

    private fun uploadToStorageFromGithub(key: ToolKey): Mono<Long> = toolStorage.upload(key, downloadFromGithub(key))

    @PostConstruct
    private fun loadToStorage() = toolStorage.list().collectList().flatMapIterable { availableFiles ->
        supportedTools.filter {
            it !in availableFiles
        }
    }
        .flatMap { key ->
            uploadToStorageFromGithub(key)
//        toolStorage.upload(key, ClassPathResource(key.executableName).toByteBufferFlux()).thenReturn(key)
        }
        .subscribe()

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos"
        private const val LATEST_VERSION = "latest"
        private val supportedTools = listOf(
            ToolKey("saveourtool", "diktat", "v1.2.3", "diktat-1.2.3.jar"),
            ToolKey("pinterest", "ktlint", "0.46.1", "ktlint"),
        )
        private fun httpClient(): HttpClient = HttpClient {
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
                this.socketTimeoutMillis = socketTimeoutMillis
            }
            install(ContentNegotiation) {
                val json = Json { ignoreUnknownKeys = true }
                json(json)
            }
        }
    }
}

