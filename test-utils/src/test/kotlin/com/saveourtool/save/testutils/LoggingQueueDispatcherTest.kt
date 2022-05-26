package com.saveourtool.save.testutils

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.Socket

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoggingQueueDispatcherTest {
    private val dispatcher = LoggingQueueDispatcher()
    @BeforeAll

    @BeforeEach
    fun cleanup() {
        dispatcher.cleanup()
    }

    @Test
    @Suppress("UnsafeCallOnNullableType", "UseEmptyCounterpart")
    fun `enqueued responses should be matched with requests`() {
        val pathRegex = "/(\\w)+(\\d)+"
        dispatcher.enqueueResponse(pathRegex, MockResponse().setResponseCode(200))
        val pathString = "/example321"
        val request = RecordedRequest(
            "GET $pathString ",
            MockResponse().headers,
            emptyList(),
            0,
            Buffer().write("GET $pathString ".toByteArray()),
            0,
            Socket(),
            null
        )
        val response = dispatcher.dispatch(request)
        assertTrue(response.status == "HTTP/1.1 200 OK")

        assertNotNull(dispatcher.responses[pathRegex])
        assertTrue(dispatcher.responses[pathRegex]!!.peek() == null)
    }

    @Test
    fun `should enqueue responses`() {
        dispatcher.enqueueResponse("/path1", MockResponse().setResponseCode(200))
        dispatcher.enqueueResponse("/path2", MockResponse().setResponseCode(200))
        dispatcher.enqueueResponse("/(\\w)+(\\d)+", MockResponse().setResponseCode(200))
        dispatcher.enqueueResponse("regex", MockResponse().setResponseCode(200))

        assertNotNull(dispatcher.responses["/path1"])
        assertNotNull(dispatcher.responses["/path2"])
        assertNotNull(dispatcher.responses["/(\\w)+(\\d)+"])
        assertNotNull(dispatcher.responses["regex"])

        assertTrue(dispatcher.responses.size == 4)
    }

    @Test
    @Suppress("UnsafeCallOnNullableType", "UseEmptyCounterpart")
    fun `priority of enqueued responses should be higher that defaults one`() {
        val defaultPathRegex = "/users([/])?"
        dispatcher.setDefaultResponseForPath(defaultPathRegex, MockResponse().setResponseCode(403))
        val vertoletPathString = "/users/sanyavertolet"
        dispatcher.enqueueResponse(vertoletPathString, MockResponse().setResponseCode(200))
        val permittedRequest = RecordedRequest(
            "GET $vertoletPathString ",
            MockResponse().headers,
            emptyList(),
            0,
            Buffer().write("GET $vertoletPathString ".toByteArray()),
            0,
            Socket(),
            null
        )
        val okResponse = dispatcher.dispatch(permittedRequest)
        assertTrue(okResponse.status == "HTTP/1.1 200 OK")

        val defaultPathString = "/users/"
        val forbiddenRequest = RecordedRequest(
            "GET $defaultPathString ",
            MockResponse().headers,
            emptyList(),
            0L,
            Buffer().write("GET $defaultPathString ".toByteArray()),
            0,
            Socket(),
            null
        )

        val failResponse = dispatcher.dispatch(forbiddenRequest)
        assertTrue(failResponse.status == "HTTP/1.1 403 Client Error")
    }
}
