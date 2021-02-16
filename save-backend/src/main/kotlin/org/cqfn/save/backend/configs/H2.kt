package org.cqfn.save.backend.configs

import org.slf4j.LoggerFactory
import org.h2.tools.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.sql.SQLException

/**
 * This class is used for creation of h2 console on port 8081
 */
@Component
class H2 {
    private var webServer: Server? = null
    private val log = LoggerFactory.getLogger(H2::class.java)

    @Value("\${savebackend.h2-console-port}")
    var h2ConsolePort: Int? = null

    @EventListener(ContextRefreshedEvent::class)
    @Throws(SQLException::class)
    fun start() {
        log.info("starting h2 console at port $h2ConsolePort")
        webServer = Server.createWebServer("-webPort", h2ConsolePort.toString()).start()
    }

    @EventListener(ContextClosedEvent::class)
    fun stop() {
        log.info("stopping h2 console at port $h2ConsolePort")
        webServer?.stop()
    }
}