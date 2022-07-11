package com.saveourtool.save.frontend.components

import csstype.ClassName
import generated.SAVE_VERSION
import react.*
import react.dom.br
import react.dom.div
import react.dom.footer
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.footer
import react.dom.html.ReactHTML.span
import react.dom.span

/**
 * A web page footer component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
class Footer : Component<Props, State>() {
    override fun render(): ReactNode = this::class.react.create {
        footer {
            className = ClassName("sticky-footer bg-white")
            div {
                className = ClassName("container my-auto")
                div {
                    className = ClassName("copyright text-center my-auto")
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
