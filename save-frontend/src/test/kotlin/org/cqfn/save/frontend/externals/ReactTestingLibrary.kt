/**
 * Declarations from https://github.com/testing-library/dom-testing-library,
 */

@file:JsModule("@testing-library/react")
@file:JsNonModule

package org.cqfn.save.frontend.externals

import org.w3c.dom.HTMLElement
import react.Props
import react.ReactElement
import kotlin.js.Promise

external fun <P : Props> render(ui: ReactElement<P>, options: dynamic = definedExternally): RenderResult

external class RenderResult {
    var container: dynamic
}

external class BoundFunctions {
    fun <T : HTMLElement> getByText(text: String, options: dynamic = definedExternally): T

    fun <T : HTMLElement> findByText(text: String, options: dynamic = definedExternally): Promise<T>

    fun <T : HTMLElement?> queryByText(text: String, options: dynamic = definedExternally): T?
}

/**
 * https://testing-library.com/docs/queries/about/#screen
 * https://github.com/testing-library/dom-testing-library/blob/main/types/screen.d.ts
 */
external val screen: BoundFunctions

