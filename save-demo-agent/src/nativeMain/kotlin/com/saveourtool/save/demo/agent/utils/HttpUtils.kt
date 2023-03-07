/**
 * Utils to send http requests
 */

package com.saveourtool.save.demo.agent.utils

import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.logging.logInfo
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.core.utils.runIf
import com.saveourtool.save.demo.DemoAgentConfig
import com.saveourtool.save.utils.failureOrNotOk
import com.saveourtool.save.utils.fs
import com.saveourtool.save.utils.notOk
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import okio.Path
import okio.buffer
import okio.use
import kotlin.native.concurrent.AtomicLong

private val httpClient = HttpClient(CIO)

private suspend fun HttpClient.download(url: String, file: Path): Result<HttpResponse> = runCatching {
    prepareGet {
        url(url)
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
    }
        .execute { httpResponse ->
            if (httpResponse.status.isSuccess()) {
                val channel: ByteReadChannel = httpResponse.body()
                val totalBytes = AtomicLong(0L)
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_HTTP_BUFFER_SIZE.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        val hasAlreadyAnyData = totalBytes.value > 0
                        fs.appendingSink(file, mustExist = hasAlreadyAnyData).buffer().use { it.write(bytes) }
                        totalBytes.addAndGet(bytes.size.toLong())
                        logDebug("Received ${bytes.size} ($totalBytes) bytes out of ${httpResponse.contentLength()} bytes from ${httpResponse.request.url}")
                    }
                }
                if (totalBytes.value == 0L) {
                    error("Downloaded a file from $url but content is empty")
                }
            } else {
                logWarn("Skipping downloading as request is not a success: ${httpResponse.status}")
            }
            httpResponse
        }
}

/**
 * Construct url from environment variables and get the rest of configuration
 *
 * @return [DemoAgentConfig] fetched from server
 */
suspend fun getConfiguration(): DemoAgentConfig = httpClient.get {
    val hostName = getEnvOrNotFound(DemoAgentConfig.DEMO_URL_ENV)
    val organizationName = getEnvOrNotFound(DemoAgentConfig.DEMO_ORGANIZATION_ENV)
    val projectName = getEnvOrNotFound(DemoAgentConfig.DEMO_PROJECT_ENV)
    val version = getEnvOrNotFound(DemoAgentConfig.DEMO_VERSION_ENV)
    url(
        URLBuilder(
            host = hostName,
            pathSegments = listOf(
                "demo",
                "internal",
                "manager",
                organizationName,
                projectName,
                "configure-me",
            ),
            parameters = Parameters.build { append("version", version) }
        ).build()
    )
}.body()

/**
 * @param fileLabel
 * @param url
 * @param target
 */
suspend fun download(fileLabel: String, url: String, target: Path) {
    logDebug("Will now download $fileLabel from $url into $target")
    val result = demoRequestWrapper {
        httpClient.download(
            url = url,
            file = target,
        )
    }
    if (result.failureOrNotOk()) {
        logError("Couldn't download $fileLabel from $url")
    }

    logInfo("Downloaded $fileLabel (resulting size = ${fs.metadata(target).size} bytes) from $url into $target")
}

private suspend fun demoRequestWrapper(
    requestToBackend: suspend () -> Result<HttpResponse>
): Result<HttpResponse> = requestToBackend().runIf({ failureOrNotOk() }) {
    val reason = if (notOk()) {
        "save-demo returned status ${getOrNull()?.status}"
    } else {
        if (exceptionOrNull() is CancellationException) {
            "Request has been interrupted, terminating."
        } else {
            "save-demo is unreachable, ${exceptionOrNull()?.message}"
        }
    }
    // logError("Cannot process request to save-demo: $reason")
    throw IllegalStateException("Cannot process request to save-demo: $reason")
}
