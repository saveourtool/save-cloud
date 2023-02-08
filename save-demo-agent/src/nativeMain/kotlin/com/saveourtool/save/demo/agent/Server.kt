/**
 * save-demo-agent server configuration
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.save.core.logging.logInfo
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.agent.utils.setupEnvironment
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.reflect.*
import kotlinx.coroutines.CompletableDeferred

private fun Routing.alive(configuration: CompletableDeferred<DemoAgentConfig>) = get("/alive") {
    call.respond(if (configuration.isCompleted) {
        HttpStatusCode.OK
    } else {
        HttpStatusCode.Created
    })
}

private fun Routing.configure(updateConfig: (DemoAgentConfig) -> Unit) = post("/setup") {
    val config = call.receive<DemoAgentConfig>().also(updateConfig)
    logInfo("Agent has received configuration.")
    try {
        setupEnvironment(config.demoUrl, config.demoConfiguration)
        call.respondText(
            "Agent is set up.",
            status = HttpStatusCode.OK,
        )
    } catch (exception: IllegalStateException) {
        call.respondText(
            exception.message ?: "Internal agent error.",
            status = HttpStatusCode.InternalServerError,
        )
    }
}

private fun Routing.run(config: CompletableDeferred<DemoAgentConfig>) = post("/run") {
    if (!config.isCompleted) {
        call.respond(HttpStatusCode.FailedDependency)
    }
    val runRequest: DemoRunRequest = call.receive()
    val result: DemoResult = runDemo(runRequest, config)
    call.respond(result)
}

/**
 * Configure ktor server (runs on [CIO] engine) with [serverConfiguration]
 *
 * @param serverConfiguration information required to configure ktor server
 * @return [CIOApplicationEngine]
 */
fun server(serverConfiguration: ServerConfiguration) = embeddedServer(CIO, port = serverConfiguration.port.toInt()) {
    install(ContentNegotiation) { json() }
    routing {
        val deferredConfig: CompletableDeferred<DemoAgentConfig> = CompletableDeferred()
        alive(deferredConfig)
        configure { deferredConfig.complete(it) }
        run(deferredConfig)
    }
}
