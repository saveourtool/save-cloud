package com.saveourtool.save.utils.github

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json

/**
 * A singleton to download assets from GitHub
 */
object GitHubHelper {
    /**
     * A base url for GitHub api
     */
    const val API_URL = "https://api.github.com/repos"

    /**
     * Version which specified the latest version
     */
    const val LATEST_VERSION = "latest"
    private val httpClient = httpClient()

    private suspend fun getMetadata(repo: GitHubRepo, tagName: String): ReleaseMetadata = httpClient.get(repo.getMetadataUrl(tagName)).body()

    private suspend fun downloadAsset(asset: ReleaseAsset): ByteReadChannel = httpClient.get {
        url(asset.downloadUrl)
        accept(asset.contentType())
    }
        .bodyAsChannel()

    /**
     * Downloads asset from GitHub in provided [repo] with provided [tagName]
     * It expects that there is only a single asset
     *
     * @param repo
     * @param tagName
     * @return content of found asset with its size
     */
    suspend fun download(repo: GitHubRepo, tagName: String): Pair<ByteReadChannel, Long> = getMetadata(repo, tagName)
        .assets
        .single {
            !it.isDigest()
        }
        .let {
            downloadAsset(it) to it.size
        }

    private fun httpClient(): HttpClient = HttpClient {
        install(ContentNegotiation) {
            val json = Json { ignoreUnknownKeys = true }
            json(json)
        }
    }
}
