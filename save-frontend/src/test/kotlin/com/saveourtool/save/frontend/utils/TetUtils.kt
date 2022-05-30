package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.errorStatusContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.Response
import react.FC
import react.PropsWithChildren
import react.useState

val wrapper = FC<PropsWithChildren> {
    val (_, setMockState) = useState<Response?>(null)
    errorStatusContext.Provider {
        value = setMockState
        +it.children
    }
}

/**
 * Mocks a successful response with serialized value of [value] and returns the same [response]
 */
inline fun <reified T> mockMswResponse(response: dynamic, value: T): dynamic {
    response.status = 200
    response.headers.set("Content-Type", "application/json")
    response.body = Json.encodeToString(value)
    return response
}
