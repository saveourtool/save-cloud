package org.cqfn.save.frontend.components

import generated.SAVE_VERSION
import react.Props
import react.RBuilder
import react.RComponent
import react.State
import react.dom.br
import react.dom.div
import react.dom.footer
import react.dom.span

/**
 * A web page footer component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
class Footer : RComponent<Props, State>() {
    override fun RBuilder.render() {
        footer("sticky-footer bg-white") {
            div("container my-auto") {
                div("copyright text-center my-auto") {
                    span {
                        +"Copyright ${js("String.fromCharCode(169)")} SAVE 2021-2022"
                        br {}
                        +"Version $SAVE_VERSION"
                    }
                }
            }
        }
    }
}
