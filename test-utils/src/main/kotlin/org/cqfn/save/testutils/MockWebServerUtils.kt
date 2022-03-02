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
    private val defaultResponses: ConcurrentMap<String, MockResponse> = ConcurrentHashMap()
    private var failFastResponse: MockResponse? = MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)

    private fun getMethodPath(fullPath: String?) = fullPath?.let { Regex("/[\\w]*").find(it)?.value ?: "" } ?: ""

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = getMethodPath(request.path)
        println("${request.path} -> $path")
        val result = if (defaultResponses[path] != null) {
            logger.info("Default response [${defaultResponses[path]}] exists for path [$path].")
            defaultResponses[path]!!
        } else if (failFastResponse != null && responses[path]?.peek() == null) {
            logger.info("No response is present in queue with path [$path].")
            println("Error!")
            return failFastResponse!!
        } else {
            responses[path]!!.take()
        }

        if (result == deadLetter) {
            responses[path]!!.add(deadLetter)
        }

        return result.also { logger.info("Response [$result] was taken for request [$request].") }
    }

    fun setDefaultResponseForPath(path: String, defaultMockResponse: MockResponse) {
        defaultResponses[path] = defaultMockResponse
    }

    override fun shutdown() {
        responses.values.forEach { it.add(deadLetter) }
    }

    override fun peek(): MockResponse = responses.values
        .filter { it.isNotEmpty() }
        .firstNotNullOfOrNull { it.peek() }
        ?: failFastResponse
        ?: super.peek()

    fun enqueueResponse(fullPath: String, response: MockResponse) {
        val path = getMethodPath(fullPath)
        if (responses[path] == null) {
            responses[path] = LinkedBlockingQueue<MockResponse>().apply { add(response) }
            logger.info("Added LinkedBlockingQueue for a new path [$path] and put there [$response]. " +
                    "Now there are ${responses[path]!!.count()} responses.")
        } else {
            responses[path]!!.add(response)
            logger.info("Added [$response] into queue with path [$path]")
        }
    }
    fun isQueueEmpty() = responses.keys.all { isQueueEmpty(it) }
    private fun isQueueEmpty(path: String): Boolean = responses[getMethodPath(path)]?.isEmpty() ?: true

    companion object {
        private val deadLetter = MockResponse().apply {
            this.status = "HTTP/1.1 ${HttpURLConnection.HTTP_UNAVAILABLE} shutting down"
        }
    }
}

/**
 * @param path path to store enqueued response
 * @param response response to enqueue
 * @throws ClassCastException
 */
fun MockWebServer.enqueue(path: String, response: MockResponse) {
    if (dispatcher is LoggingQueueDispatcher) {
        (dispatcher as LoggingQueueDispatcher).enqueueResponse(path, response.clone())
    } else {
        throw ClassCastException("dispatcher type should be LoggingQueueDispatcher")
    }
}

fun MockWebServer.isQueueEmpty(): Boolean = (dispatcher as LoggingQueueDispatcher).isQueueEmpty()

/**
 * @param path
 * @param response
 */
fun MockWebServer.setDefaultResponseForPath(path: String, response: MockResponse) =
    (dispatcher as LoggingQueueDispatcher).setDefaultResponseForPath(path, response)

/**
 * @param logger logger with which additional debug info is passed
 * @return MockWebServer used for testing
 */
fun createMockWebServer(logger: Logger) = MockWebServer().apply {
    dispatcher = LoggingQueueDispatcher(logger)
}
