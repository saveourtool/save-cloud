/**
 * save-demo-agent server configuration
 */

package com.saveourtool.save.demo.agent

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun Routing.aliveRoute() = get("/alive") { call.respond(HttpStatusCode.OK) }

private fun Routing.runRoute() = post("/run") {
    call.respondText { "Run route" }
}

/**
 * Configure ktor server (runs on [CIO] engine) with [serverConfiguration]
 *
 * @param serverConfiguration information required to configure ktor server
 * @return [CIOApplicationEngine]
 */
fun server(serverConfiguration: ServerConfiguration) = embeddedServer(CIO, port = serverConfiguration.port) {
    routing {
        aliveRoute()
        runRoute()
    }
}
