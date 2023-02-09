/**
 * Contains custom react hooks
 *
 * Keep in mind that hooks could only be used from functional components!
 */

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.requestStatusContext

import js.core.jso
import org.w3c.dom.EventSource
import org.w3c.dom.EventSource.Companion.CONNECTING
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.EffectBuilder
import react.useContext
import react.useEffect
import react.useState

import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion

/**
 * Runs the provided [action] only once of first render
 *
 * @param action
 */
fun useOnce(action: () -> Unit) {
    val useOnceAction = useOnceAction()
    useOnceAction {
        action()
    }
}

/**
 * @return action which will be run once per function component
 */
fun useOnceAction(): (() -> Unit) -> Unit {
    val (isFirstRender, setFirstRender) = useState(true)
    return { action ->
        if (isFirstRender) {
            action()
            setFirstRender(false)
        }
    }
}

/**
 * Custom hook to enable tooltips.
 */
fun useTooltip() {
    useEffect {
        enableTooltip()
    }
}

/**
 * Custom hook to enable tooltips and popovers.
 */
fun useTooltipAndPopover() {
    useEffect {
        enableTooltipAndPopover()
        return@useEffect
    }
}

/**
 * Hook to get callbacks to perform requests in functional components.
 *
 * @param request
 * @return a function to trigger request execution.
 */
fun <R> useDeferredRequest(
    request: suspend WithRequestStatusContext.() -> R,
): () -> Unit {
    val scope = CoroutineScope(Dispatchers.Default)
    val context = useRequestStatusContext()
    val (isSending, setIsSending) = useState(false)
    useEffect(isSending) {
        if (!isSending) {
            return@useEffect
        }
        scope.launch {
            request(context)
            setIsSending(false)
        }.invokeOnCompletion {
            if (it != null && it !is CancellationException) {
                setIsSending(false)
            }
        }
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }
    val initiateSending: () -> Unit = {
        if (!isSending) {
            setIsSending(true)
        }
    }
    return initiateSending
}

/**
 * Hook to perform requests in functional components.
 *
 * @param dependencies
 * @param request
 */
fun <R> useRequest(
    dependencies: Array<dynamic> = emptyArray(),
    request: suspend WithRequestStatusContext.() -> R,
) {
    val scope = CoroutineScope(Dispatchers.Default)
    val context = useRequestStatusContext()

    useEffect(*dependencies) {
        scope.launch {
            request(context)
        }
        cleanup {
            if (scope.isActive) {
                scope.cancel()
            }
        }
    }
}

/**
 * @return [WithRequestStatusContext] implementation
 */
@Suppress("TOO_LONG_FUNCTION", "MAGIC_NUMBER")
fun useRequestStatusContext(): WithRequestStatusContext {
    val statusContext = useContext(requestStatusContext)
    val context = object : WithRequestStatusContext {
        override val coroutineScope = CoroutineScope(Dispatchers.Default)
        override fun setResponse(response: Response) = statusContext.setResponse(response)
        override fun setRedirectToFallbackView(isNeedRedirect: Boolean, response: Response) = statusContext.setRedirectToFallbackView(
            isNeedRedirect && response.status == 404.toShort()
        )
        override fun setLoadingCounter(transform: (oldValue: Int) -> Int) = statusContext.setLoadingCounter(transform)
    }
    return context
}

/**
 * @param colorStyle page style
 */
fun useBackground(colorStyle: Style) {
    useEffect {
        document.getElementById("content-wrapper")?.setAttribute(
            "style",
            "background: ${colorStyle.globalBackground}"
        )
        configureTopBar(colorStyle)
    }
}

/**
 * Creates a callback that is run synchronously.
 *
 * Only works inside functional components.
 *
 * @param effect the callback body (executed under [useEffect]).
 * @return a lambda that triggers the callback.
 * @see useEffect
 */
fun useDeferredEffect(
    effect: EffectBuilder.() -> Unit,
): () -> Unit {
    var isRunning by useState(initialValue = false)

    useEffect(isRunning) {
        if (!isRunning) {
            return@useEffect
        }

        effect()

        isRunning = false
    }

    return {
        if (!isRunning) {
            isRunning = true
        }
    }
}

/**
 * Reads the response of `text/event-stream` `Content-Type`.
 * _Server-Sent Events_ (SSE) are limited to `HTTP GET` method.
 *
 * Only works inside functional components.
 *
 * @param url the URL that accepts an `HTTP GET` and can respond with a
 *   `text/event-stream` `Content-Type`.
 * @param withCredentials whether
 *   [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
 *   should be allowed, the default is `false`.
 * @param eventType the event type selector.
 *   The same HTTP endpoint may return events of different types, and this
 *   selector allows to receive only a certain subset of the whole event volume.
 *   The default is `message`.
 *   On the server side, use `org.springframework.http.codec.ServerSentEvent` to
 *   create an event with a custom type.
 * @param init invoked before an event stream is requested.
 *   Allowed to change the state of components.
 * @param onCompletion invoked if the response is `HTTP 200 OK`, and when the
 *   server closes the connection.
 *   Allowed to change the state of components.
 * @param onError invoked when the event source reports an error.
 *   Allowed to change the state of components.
 * @param onEvent invoked when a new event arrives.
 *   Allowed to change the state of components.
 * @return a lambda that triggers the callback.
 * @see useNdjson
 */
@Suppress(
    "LongParameterList",
    "TOO_MANY_PARAMETERS",
    "TYPE_ALIAS",
)
fun useEventStream(
    url: String,
    withCredentials: Boolean = false,
    eventType: String = "message",
    init: EffectBuilder.() -> Unit = {},
    onCompletion: EffectBuilder.() -> Unit = {},
    onError: EffectBuilder.(error: Event, readyState: Short) -> Unit = { _, _ -> },
    onEvent: EffectBuilder.(message: MessageEvent) -> Unit,
): () -> Unit =
        useDeferredEffect {
            init()

            val source = EventSource(
                url = url,
                eventSourceInitDict = jso {
                    this.withCredentials = withCredentials
                },
            )

            source.addEventListener(eventType, { event ->
                onEvent(event as MessageEvent)
            })

            source.onerror = { error ->
                if (source.readyState == CONNECTING) {
                    source.close()
                    onCompletion()
                } else {
                    onError(error, source.readyState)
                }
            }
        }

/**
 * Reads the response of `application/x-ndjson` `Content-Type`.
 *
 * Only works inside functional components.
 *
 * @param url the URL that accepts an `HTTP GET` and can respond with a
 *   `application/x-ndjson` `Content-Type`.
 * @param init invoked before an event stream is requested.
 *   Allowed to change the state of components.
 * @param onCompletion invoked if the response is `HTTP 200 OK`, and when the
 *   server closes the connection.
 *   Allowed to change the state of components.
 * @param onError invoked when the event source reports an error.
 *   Allowed to change the state of components.
 * @param onEvent invoked when a new event arrives.
 *   Allowed to change the state of components.
 * @return a lambda that triggers the callback.
 * @see useEventStream
 */
internal fun useNdjson(
    url: String,
    init: () -> Unit = {},
    onCompletion: () -> Unit = {},
    onError: suspend (response: Response) -> Unit = { _ -> },
    onEvent: (message: String) -> Unit,
): () -> Unit =
        useDeferredRequest {
            init()

            val response = get(
                url = url,
                params = jso(),
                headers = Headers(jso {
                    Accept = "application/x-ndjson"
                }),
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )

            when {
                response.ok -> response
                    .readLines()
                    .filter(String::isNotEmpty)
                    .onCompletion {
                        onCompletion()
                    }
                    .collect(onEvent)

                else -> onError(response)
            }
        }
