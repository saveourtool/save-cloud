/**
 * Utilities for testing frontend
 */

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.requestStatusContext
import js.core.jso

import org.w3c.fetch.Response
import react.FC
import react.PropsWithChildren
import react.useState
import web.timers.setTimeout

import kotlin.js.Promise
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.create
import react.router.createMemoryRouter
import react.router.dom.RouterProvider

val wrapper: FC<PropsWithChildren> = FC { props ->
    stubInitI18n()
    val (_, setMockState) = useState<Response?>(null)
    val (_, setRedirectToFallbackView) = useState(false)
    val (_, setLoadingCounter) = useState(0)
    RouterProvider {
        router = createMemoryRouter(
            routes = arrayOf(
                jso {
                    index = true
                    element = FC {
                        requestStatusContext.Provider {
                            value = RequestStatusContext(setMockState, setRedirectToFallbackView, setLoadingCounter)
                            +props.children
                        }
                    }.create()
                }
            ),
            opts = jso {
                initialEntries = arrayOf("/")
            }
        )
    }
}

/**
 * Mocks a successful response with serialized value of [value] and returns the same [response]
 *
 * @param response a response object from MSW library
 * @param value value to be added into response body
 * @return response object with configuration applied
 */
inline fun <reified T> mockMswResponse(response: dynamic, value: T): dynamic {
    response.status = 200
    response.headers.set("Content-Type", "application/json")
    response.body = Json.encodeToString(value)
    return response
}

/**
 * @param millis the time to wait, in milliseconds.
 * @return the created `Promise` instance.
 */
fun wait(millis: Int) = Promise { resolve, _ ->
    setTimeout({ resolve(Unit) }, millis)
}

/**
 * Stub `initI18n` for testing purposes
 */
internal fun stubInitI18n() {
    val i18n: dynamic = kotlinext.js.require("i18next");
    val reactI18n: dynamic = kotlinext.js.require("react-i18next");
    val i18nResources: dynamic = jso {
        en = jso {
            translation = undefined
        }
    }

    i18n.use(reactI18n.initReactI18next).init(jso {
        resources = i18nResources
        lng = "en"
        fallbackLng = "en"
    })
}
