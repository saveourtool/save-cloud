package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.errorStatusContext
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
