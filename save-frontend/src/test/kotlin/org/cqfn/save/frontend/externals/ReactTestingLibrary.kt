/**
 * Declarations from https://github.com/testing-library/dom-testing-library
 * https://github.com/testing-library/react-testing-library/blob/main/types/index.d.ts
 */

@file:JsModule("@testing-library/react")
@file:JsNonModule
@file:Suppress(
    "USE_DATA_CLASS",
    "MISSING_KDOC_ON_FUNCTION",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_TOP_LEVEL",
    "KDOC_NO_EMPTY_TAGS",
    "KDOC_WITHOUT_PARAM_TAG",
    "KDOC_WITHOUT_RETURN_TAG",
)

package org.cqfn.save.frontend.externals

import org.w3c.dom.HTMLElement
import react.Props
import react.ReactElement
import kotlin.js.Promise

/**
 * https://testing-library.com/docs/queries/about/#screen
 * https://github.com/testing-library/dom-testing-library/blob/main/types/screen.d.ts
 */
external val screen: BoundFunctions

external class RenderResult {
    var container: dynamic
}

/**
 * https://github.com/testing-library/dom-testing-library/blob/main/types/queries.d.ts
 */
external class BoundFunctions {
    fun <T : HTMLElement> getByText(text: String, options: dynamic = definedExternally): T

    /**
     * https://testing-library.com/docs/queries/byrole
     */
    fun <T : HTMLElement> getByRole(vararg args: dynamic): T

    fun <T : HTMLElement> findByText(text: String, options: dynamic = definedExternally): Promise<T>

    fun <T : HTMLElement?> queryByText(text: String, options: dynamic = definedExternally): T?
}

external fun <P : Props> render(ui: ReactElement<P>, options: dynamic = definedExternally): RenderResult
