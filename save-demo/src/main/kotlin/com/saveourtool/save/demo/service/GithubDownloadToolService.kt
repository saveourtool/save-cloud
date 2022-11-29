package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entities.ReleaseAsset
import com.saveourtool.save.demo.entities.ReleaseMetadata
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.storage.ToolStorage
import com.saveourtool.save.demo.utils.toByteBufferFlux

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toMono

import java.nio.ByteBuffer
import javax.annotation.PostConstruct

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Service that downloads tools from github when starting
 */
@Service
class GithubDownloadToolService(
    private val toolStorage: ToolStorage,
) {
    private val jsonSerializer = Json { ignoreUnknownKeys = true }

    @Suppress("InjectDispatcher")
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val httpClient = httpClient()

    private fun getGithubMetadataUrlByKey(key: ToolKey) = with(key) {
        val release = if (version == LATEST_VERSION) {
            version
        } else {
            "tags/$version"
        }
        "$GITHUB_API_URL/$ownerName/$toolName/releases/$release"
    }

    private fun getMetadata(key: ToolKey): ReleaseMetadata {
        val channel: Channel<ReleaseMetadata> = Channel()
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

    @Suppress("ReactiveStreamsUnusedPublisher")
    private suspend fun downloadAsset(asset: ReleaseAsset): Flux<ByteBuffer> = httpClient.get {
        url(asset.downloadUrl)
        accept(asset.contentType())
    }
        .bodyAsChannel()
        .toByteBufferFlux()

    private fun downloadFromGithubAndUploadToStorage(key: ToolKey) = getMetadata(key).assets
        .filterNot(ReleaseAsset::isDigest)
        .first()
        .let { asset ->
            scope.launch {
                toolStorage.upload(key, downloadAsset(asset)).subscribe()
            }
        }

    @PostConstruct
    private fun loadToStorage() = toolStorage.list()
        .collectList()
        .flatMapIterable { availableFiles ->
            supportedTools.filter {
                it !in availableFiles
            }
        }
        .flatMap { key ->
            downloadFromGithubAndUploadToStorage(key).toMono()
        }
        .subscribe()

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos"
        private const val LATEST_VERSION = "latest"
        private val supportedTools = listOf(
            ToolKey("pinterest", "ktlint", "0.46.1", "ktlint"),
            ToolKey("saveourtool", "diktat", "v1.2.3", "diktat-1.2.3.jar"),
        )
        private fun httpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                val json = Json { ignoreUnknownKeys = true }
                json(json)
            }
        }
    }
}
