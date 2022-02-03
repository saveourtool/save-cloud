package org.cqfn.test

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.slf4j.Logger

fun createMockWebServer(logger: Logger, isFailFast: Boolean = true) = MockWebServer().apply {
    dispatcher = LoggingQueueDispatcher(logger)
    (dispatcher as QueueDispatcher).setFailFast(isFailFast)
}

class LoggingQueueDispatcher(private val logger: Logger) : QueueDispatcher(){
    override fun dispatch(request: RecordedRequest): MockResponse {
        return super.dispatch(request).also {
            logger.info("For request ${request.requestUrl} returning mocked response $it")
        }
    }
}
