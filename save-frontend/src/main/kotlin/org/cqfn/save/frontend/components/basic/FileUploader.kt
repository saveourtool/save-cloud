/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import react.RProps
import react.dom.attrs
import react.dom.div
import react.dom.h6
import react.dom.img
import react.dom.input
import react.dom.label
import react.dom.strong
import react.fc

import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction

/**
 * Props for file uploader
 */
external interface UploaderProps : RProps {
    /**
     * List of provided files
     */
    var files: List<File>
}

/**
 * @param handler invoked on `input` change events
 * @return a RComponent
 */
fun fileUploader(handler: (HTMLInputElement) -> Unit) = fc<UploaderProps> { props ->
    div("mb-3") {
        h6(classes = "d-inline mr-3") {
            +"Select files:"
        }
        div {
            label {
                input(type = InputType.file) {
                    attrs.multiple = true
                    attrs.hidden = true
                    attrs {
                        onChangeFunction = { event ->
                            val target = event.target as HTMLInputElement
                            handler(target)
                        }
                    }
                }
                img(classes = "img-upload", src = "img/upload.svg") {}
                strong { +"Upload files:" }
                +props.files.joinToString { it.name }
            }
        }
    }
}
