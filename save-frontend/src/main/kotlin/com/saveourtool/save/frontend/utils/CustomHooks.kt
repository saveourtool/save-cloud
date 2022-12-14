/**
 * Contains custom react hooks
 *
 * Keep in mind that hooks could only be used from functional components!
 */

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.requestStatusContext

import org.w3c.fetch.Response
import react.useContext
import react.useEffect
import react.useState

import kotlinx.browser.document
import kotlinx.coroutines.*

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
