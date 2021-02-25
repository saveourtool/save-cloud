package org.cqfn.save.backend.configs

import org.h2.tools.Server
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

import java.sql.SQLException

/**
 * This class is used for creation of h2 console on port 8081
 *
 * @property h2ConsolePort port of the console(8081)
 */
@Component
@Profile("dev")
class H2 {
    private val log = LoggerFactory.getLogger(H2::class.java)
    private var webServer: Server? = null

    /**
     * Specifies port on which console is running
     */
    @Value("\${save.backend.h2-console-port}")
    var h2ConsolePort: Int? = null

    /**
     * Works when the console starts
     */
    @EventListener(ContextRefreshedEvent::class)
    @Throws(SQLException::class)
    fun start() {
        log.info("starting h2 console at port $h2ConsolePort")
        webServer = Server.createWebServer("-webPort", h2ConsolePort.toString()).start()
    }

    /**
     * When stop event is occurred, this method is running
     */
    @EventListener(ContextClosedEvent::class)
    fun stop() {
        log.info("stopping h2 console at port $h2ConsolePort")
        webServer?.stop()
    }
}
