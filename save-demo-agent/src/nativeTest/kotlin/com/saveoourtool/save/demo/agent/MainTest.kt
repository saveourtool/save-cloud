@file:Suppress("MagicNumber")
package com.saveoourtool.save.demo.agent

import com.saveourtool.save.demo.agent.runServerOnPort
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.test.*

class MainTest {
    private val serverUrl = "localhost"
    private val serverPort = 23456
    private val server = runServerOnPort(23456)

    @BeforeTest
    fun startServer() {
        server.start()
    }

    @AfterTest
    fun stopServer() {
        server.stop()
    }

    @Test
    fun testServerStartup() {
        HttpClient(CIO) {
            engine { requestTimeout = 500 }
        }
            .use { client ->
                val scope = CoroutineScope(Dispatchers.Default)
                scope.launch {
                    assert(client.get("$serverUrl:$serverPort/alive").status.isSuccess())
                }
            }
    }
}
