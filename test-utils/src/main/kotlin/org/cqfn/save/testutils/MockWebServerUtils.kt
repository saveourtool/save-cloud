@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.testutils

import okhttp3.mockwebserver.*
import org.slf4j.Logger
import java.net.HttpURLConnection
import java.util.concurrent.*

private typealias ResponsesMap = ConcurrentMap<String, BlockingQueue<MockResponse>>

/**
 * Queue dispatcher with additional logging
 *
 * @param logger
 */
class LoggingQueueDispatcher(private val logger: Logger) : Dispatcher() {
    private val responses: ResponsesMap = ConcurrentHashMap()
    private var failFastResponse: MockResponse? = MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path?.substringBefore("?")
        // println("${request.path} -> $path")
        if (failFastResponse != null && responses[path]?.peek() == null) {
            println("Error")
            return failFastResponse!!
        }
        val result = responses[path]!!.take()
        logger.info("Response [$result] was taken from responseQueue for request [$request].")

        if (result == deadLetter) {
            responses[path]!!.add(deadLetter)
        }

        return result
    }

    override fun shutdown() {
        responses.values.forEach { it.add(deadLetter) }
    }

    override fun peek(): MockResponse = responses.values
        .filter { it.isNotEmpty() }
        .firstNotNullOfOrNull { it.peek() }
        ?: failFastResponse
        ?: super.peek()

    /**
     * @param path
     * @param response
     */
    fun enqueueResponse(path: String, response: MockResponse) {
        responses[path]?.add(response) ?: responses.put(path, LinkedBlockingQueue<MockResponse>().apply { add(response) })
    }

    companion object {
        private val deadLetter = MockResponse().apply {
            this.status = "HTTP/1.1 ${HttpURLConnection.HTTP_UNAVAILABLE} shutting down"
        }
    }
}

/**
 * @param path
 * @param response
 * @throws ClassCastException
 */
fun MockWebServer.enqueue(path: String, response: MockResponse) {
    if (dispatcher is LoggingQueueDispatcher) {
        (dispatcher as LoggingQueueDispatcher).enqueueResponse(path, response.clone())
    } else {
        throw ClassCastException("dispatcher type should be LoggingQueueDispatcher")
    }
}

/**
 * @param logger logger with which additional debug info is passed
 * @return MockWebServer used for testing
 */
fun createMockWebServer(logger: Logger) = MockWebServer().apply {
    dispatcher = LoggingQueueDispatcher(logger)
    // dispatcher.enqueueResponse(path, response)
}
