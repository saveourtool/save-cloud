@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.testutils

import okhttp3.mockwebserver.*
import org.junit.Assert.assertTrue
import org.slf4j.Logger
import java.net.HttpURLConnection
import java.nio.charset.Charset
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

    private fun getMethodPath(fullPath: String?) = fullPath?.let { Regex("/[\\w]*").find(it)?.value } ?: ""

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = getMethodPath(request.path)
        val result = if (defaultResponses[path] != null) {
            logger.info("Default response [${defaultResponses[path]}] exists for path [$path].")
            defaultResponses[path]!!
        } else if (failFastResponse != null && responses[path]?.peek() == null) {
            logger.info("No response is present in queue with path [$path].")
            return failFastResponse!!
        } else {
            responses[path]!!.take()
        }

        if (result == deadLetter) {
            responses[path]!!.add(deadLetter)
        }

        return result.also { logger.info("Response [$result] was taken for request [$request].") }
    }

    /**
     * @param path
     * @param defaultMockResponse
     */
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

    /**
     * @param fullPath to method that should cause `response`
     * @param response that will be added to queue that matches `fullPath`
     */
    fun enqueueResponse(fullPath: String, response: MockResponse) {
        val path = getMethodPath(fullPath)
        responses[path]?.let {
            responses[path]!!.add(response)
            logger.info("Added [$response] into queue with path [$path]")
        }
            ?: run {
                responses[path] = LinkedBlockingQueue<MockResponse>().apply { add(response) }
                logger.info("Added LinkedBlockingQueue for a new path [$path] and put there [$response]. " +
                        "Now there are ${responses[path]!!.count()} responses.")
            }
    }

    /**
     * Checks if there is any response in each queue
     */
    fun checkQueues() {
        responses.keys.forEach { checkQueue(it) }
    }

    private fun checkQueue(path: String) = responses[getMethodPath(path)]?.peek()?.let { mockResponse ->
        val errorMessage = "There is an enqueued response in the MockServer after a test has completed." +
                "Enqueued body: ${mockResponse.getBody()?.readString(Charset.defaultCharset())}"
        assertTrue(errorMessage, mockResponse.getBody().let { it == null || it.size == 0L })
    }

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

/**
 * Checks if there are any MockResponses left in any path queue
 */
fun MockWebServer.checkQueues() {
    (dispatcher as LoggingQueueDispatcher).checkQueues()
}

/**
 * Sets default MockResponse for certain path
 *
 * @param path
 * @param response
 */
fun MockWebServer.setDefaultResponseForPath(path: String, response: MockResponse) =
        (dispatcher as LoggingQueueDispatcher).setDefaultResponseForPath(path, response)

/**
 * Creates MockWebServer with LoggingQueueDispatcher
 *
 * @param logger logger with which additional debug info is passed
 * @return MockWebServer used for testing
 */
fun createMockWebServer(logger: Logger) = MockWebServer().apply {
    dispatcher = LoggingQueueDispatcher(logger)
}
