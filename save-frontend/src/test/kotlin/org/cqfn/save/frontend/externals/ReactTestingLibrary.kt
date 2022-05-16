@file:JsModule("@testing-library/react")
@file:JsNonModule

package org.cqfn.save.frontend.externals

import react.Props
import react.ReactElement

external fun <P : Props> render(ui: ReactElement<P>, options: dynamic): RenderResult

@JsName("RenderResult")
external class RenderResult {
    var container: dynamic
}
