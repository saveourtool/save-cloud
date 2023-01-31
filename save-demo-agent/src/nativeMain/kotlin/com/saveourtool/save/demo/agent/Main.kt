/**
 * An entry point for save-demo-agent.
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.save.core.utils.ProcessBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import okio.FileSystem

@Suppress("MagicNumber")
fun main() {
    runServerOnPort(23456).start()
}

/**
 * @param port port number
 * @return [CIOApplicationEngine]
 */
@Suppress("MagicNumber")
internal fun runServerOnPort(port: Int) = ProcessBuilder(true, FileSystem.SYSTEM).let { pb ->
    embeddedServer(CIO, port = port) {
        routing {
            get("/alive") {
                call.respond(HttpStatusCode.OK)
            }

            get("/run") {
                call.respondText {
                    pb.exec("echo \"Hello\"", "~", null, 100)
                        .stdout
                        .joinToString("\n")
                }
            }
        }
    }
}
