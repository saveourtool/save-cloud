@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.testutils

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.slf4j.Logger

/**
 * Queue dispatcher with additional logging
 */
class LoggingQueueDispatcher(private val logger: Logger) : QueueDispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse = super.dispatch(request).also {
        logger.info("For request ${request.requestUrl} returning mocked response $it")
    }
}

/**
 * @param logger logger with which additional debug info is passed
 * @param isFailFast
 * @return MockWebServer used for testing
 */
fun createMockWebServer(logger: Logger, isFailFast: Boolean = true) = MockWebServer().apply {
    dispatcher = LoggingQueueDispatcher(logger)
    (dispatcher as QueueDispatcher).setFailFast(isFailFast)
}
