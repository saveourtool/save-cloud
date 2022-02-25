@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.testutils

import okhttp3.mockwebserver.*
import org.slf4j.Logger
import java.net.HttpURLConnection
import java.util.concurrent.*

private typealias ResponsesMap = ConcurrentMap<String, BlockingQueue<MockResponse>>
/**
 * Queue dispatcher with additional logging
 */
class LoggingQueueDispatcher(private val logger: Logger) : Dispatcher() {
    val responses: ResponsesMap = ConcurrentHashMap()
    private var failFastResponse: MockResponse? = MockResponse().setResponseCode(404)

    override fun dispatch(request: RecordedRequest): MockResponse {
        if (failFastResponse != null && responses[request.path]?.peek() == null) {
            return failFastResponse!!
        }
        val result = responses[request.path]!!.take()

        if (result == LoggingQueueDispatcher.DEAD_LETTER) {
            responses[request.path]!!.add(LoggingQueueDispatcher.DEAD_LETTER)
        }

        return result
    }

    fun setFailFast(failFast: Boolean) {
        val failFastResponse = if (failFast) {
            MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
        } else {
            null
        }
        setFailFast(failFastResponse)
    }

    fun setFailFast(failFastResponse: MockResponse?) {
        this.failFastResponse = failFastResponse
    }

    override fun shutdown() {
        responses.values.forEach{ it.add(DEAD_LETTER) }
    }

    fun peek(path: String): MockResponse {
        val response = responses[path]?.peek() ?: failFastResponse ?: super.peek()
        println("peek result: $response")
        return response
    }

    fun peekAll(): List<Pair<String, MockResponse>> =
        responses.map { (path, queue) ->
            path to peek(path)
        }

    fun enqueue(path: String, response: MockResponse): ResponsesMap {
        responses[path]?.add(response) ?: responses.put(path, LinkedBlockingQueue<MockResponse>().apply { add(response) })
        return responses
    }

    companion object {
        private val DEAD_LETTER = MockResponse().apply {
            this.status = "HTTP/1.1 ${HttpURLConnection.HTTP_UNAVAILABLE} shutting down"
        }
    }
}

fun MockWebServer.enqueue(path: String, response: MockResponse) {
    (dispatcher as LoggingQueueDispatcher).enqueue(path, response)
}

fun MockWebServer.peekAllResponses() = (dispatcher as LoggingQueueDispatcher).peekAll()

/**
 * @param logger logger with which additional debug info is passed
 * @param isFailFast
 * @return MockWebServer used for testing
 */
fun createMockWebServer(logger: Logger) = MockWebServer().apply {
    dispatcher = LoggingQueueDispatcher(logger)
    // dispatcher.enqueueResponse(path, response)
}
