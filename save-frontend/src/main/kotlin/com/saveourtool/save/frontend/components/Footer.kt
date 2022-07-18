package com.saveourtool.save.frontend.components

import com.saveourtool.save.frontend.utils.CComponent
import csstype.ClassName
import generated.SAVE_VERSION
import react.*
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.footer
import react.dom.html.ReactHTML.span

/**
 * A web page footer component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
class Footer : CComponent<Props, State>() {
    override fun ChildrenBuilder.render() {
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
