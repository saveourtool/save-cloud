package com.saveourtool.save.demo.runners.pod

import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.runners.Runner

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import kotlinx.serialization.json.Json

/**
 * Interface that should be implemented by all the runners that use Kubernetes pods in order to run tools for demo.
 */
interface PodRunner : Runner {
    /**
     * @param demoRunRequest params of type [DemoRunRequest]
     * @return tool's report as [DemoResult]
     */
    @Suppress("InjectDispatcher")
    override fun run(demoRunRequest: DemoRunRequest): Mono<DemoResult> = Mono.just(demoRunRequest)
        .doOnNext { logger.debug("Getting url for demo execution.") }
        .flatMap { getUrl(demoRunRequest) }
        .doOnNext { logger.info("Sending run request to $it.") }
        .flatMap { mono(Dispatchers.Default) { runRequest(it, demoRunRequest) } }
        .doOnNext { logger.info("Demo finished with [${it.terminationCode}] code.") }

    private suspend fun runRequest(
        url: String,
        demoRunRequest: DemoRunRequest,
    ): DemoResult? = httpClient.post(url) {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        setBody(demoRunRequest)
    }
        .let { response -> response.takeIf { it.status.isSuccess() }?.body() }

    /**
     * @param demoRunRequest
     * @return url of a pod that runs demo
     */
    fun getUrl(demoRunRequest: DemoRunRequest): Mono<String>

    companion object {
        private val logger = LoggerFactory.getLogger(PodRunner::class.java)
        private val httpClient = HttpClient(Apache) {
            install(ContentNegotiation) {
                val json = Json { ignoreUnknownKeys = true }
                json(json)
            }
        }
    }
}
