/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.domain.FileInfo
import org.cqfn.save.frontend.components.views.ConfirmationType
import org.cqfn.save.frontend.externals.fontawesome.faFile
import org.cqfn.save.frontend.externals.fontawesome.faTimesCircle
import org.cqfn.save.frontend.externals.fontawesome.faUpload
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.utils.toPrettyString

import csstype.Width
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.CSSProperties
import react.Props
import react.PropsWithChildren
import react.dom.attrs
import react.dom.button
import react.dom.div
import react.dom.input
import react.dom.label
import react.dom.li
import react.dom.option
import react.dom.select
import react.dom.span
import react.dom.strong
import react.dom.ul
import react.fc
import react.useEffect

import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onDoubleClickFunction

/**
 * Props for file uploader
 */
external interface UploaderProps : PropsWithChildren {
    /**
     * Header of the card
     */
    var header: String

    /**
     * List of files available on server side
     */
    var availableFiles: List<FileInfo>

    /**
     * List of provided files
     */
    var files: List<FileInfo>

    /**
     * Sumbit button was pressed
     */
    var isSubmitButtonPressed: Boolean?

    /**
     * state for the creation of unified confirmation logic
     */
    var confirmationType: ConfirmationType

    /**
     * General size of test suite in bytes
     */
    var suiteByteSize: Long

    /**
     * Bytes received by server
     */
    var bytesReceived: Long

    /**
     * Flag to handle uploading a file
     */
    var isUploading: Boolean
}

/**
 * @param onFileInput invoked on `input` change events
 * @param onFileSelect invoked when a file is selected from [UploaderProps.availableFiles]
 * @param onFileRemove invoked when a file is removed from selection by pushing a button
 * @param onExecutableChange when file is checked to be executable or vice versa, this handler is called
 * @return a RComponent
 */
@Suppress("TOO_LONG_FUNCTION", "TYPE_ALIAS", "LongMethod")
fun fileUploader(
    onFileSelect: (HTMLSelectElement) -> Unit,
    onFileRemove: (FileInfo) -> Unit,
    onFileInput: (HTMLInputElement) -> Unit,
    onExecutableChange: (file: FileInfo, checked: Boolean) -> Unit
) = fc<UploaderProps> { props ->
    div("mb-3") {
        div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
            +props.header
        }

        div {
            label(classes = "control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-1 pl-0") {
                +"1. Upload or select the tool (and other resources) for testing:"
            }

            ul(classes = "list-group") {
                props.files.map { fileInfo ->
                    li(classes = "list-group-item") {
                        button(classes = "btn") {
                            fontAwesomeIcon {
                                attrs.icon = faTimesCircle
                            }
                            attrs.onClickFunction = {
                                onFileRemove(fileInfo)
                            }
                        }
                        child(fileIconWithMode(fileInfo, onExecutableChange))
                        +fileInfo.toPrettyString()
                    }
                }
                li("list-group-item d-flex justify-content-between align-items-center") {
                    val wasSubmitted = props.isSubmitButtonPressed ?: false
                    val form = if (props.files.isEmpty() && wasSubmitted && props.confirmationType == ConfirmationType.NO_BINARY_CONFIRM) {
                        "form-control is-invalid"
                    } else {
                        "form-control"
                    }

                    select(classes = form) {
                        attrs.value = "default"
                        option {
                            attrs.value = "default"
                            attrs.disabled = true
                            +"Select a file from existing"
                        }
                        props.availableFiles.sortedByDescending { it.uploadedMillis }.map {
                            option("list-group-item") {
                                attrs.value = it.name
                                +it.toPrettyString()
                            }
                        }
                        attrs.onChangeFunction = {
                            onFileSelect(it.target as HTMLSelectElement)
                        }
                    }
                }
                li("list-group-item d-flex justify-content-between align-items-center") {
                    label {
                        input(type = InputType.file) {
                            attrs.multiple = true
                            attrs.hidden = true
                            attrs {
                                onChangeFunction = { event ->
                                    val target = event.target as HTMLInputElement
                                    onFileInput(target)
                                }
                            }
                        }
                        fontAwesomeIcon(icon = faUpload)
                        attrs["data-toggle"] = "tooltip"
                        attrs["data-placement"] = "top"
                        attrs["title"] = "Regular files/Executable files/ZIP Archives"
                        strong { +"Upload files:" }
                    }
                }

                div("progress") {
                    attrs.hidden = !props.isUploading
                    div("progress-bar progress-bar-striped progress-bar-animated") {
                        attrs["style"] = kotlinext.js.jsObject<CSSProperties> {
                            width = if (props.suiteByteSize != 0.toLong()) {
                                "${ (100 * props.bytesReceived / props.suiteByteSize) }%"
                            } else {
                                "100%"
                            }.unsafeCast<Width>()
                        }
                        +"${props.bytesReceived / 1024} / ${props.suiteByteSize / 1024} kb"
                    }
                }
            }
        }
    }

    useEffect(listOf<dynamic>()) {
        val jquery = kotlinext.js.require("jquery")
        jquery("[data-toggle=\"tooltip\"]").tooltip()
    }
}

/**
 * A component for file icon that changes depending on executable flag
 *
 * @param fileInfo a [FileInfo] to base the icon on
 * @param onExecutableChange a handler that is invoked when icon is clicked
 * @return a functional component
 */
@Suppress("TYPE_ALIAS", "STRING_CONCATENATION")  // https://github.com/diktat-static-analysis/diKTat/issues/1076
internal fun fileIconWithMode(fileInfo: FileInfo, onExecutableChange: (file: FileInfo, checked: Boolean) -> Unit) = fc<Props> {
    span("fa-layers mr-3") {
        attrs["data-toggle"] = "tooltip"
        attrs["data-placement"] = "top"
        attrs["title"] = "Click to mark file " + if (fileInfo.isExecutable) "regular" else "executable"
        // if file was not executable, after click it will be; and vice versa
        attrs.onClickFunction = { _ ->
            // hide previous tooltip, otherwise it gets stuck during re-render
            val jquery = kotlinext.js.require("jquery")
            jquery("[data-toggle=\"tooltip\"]").tooltip("hide")
            onExecutableChange(fileInfo, !fileInfo.isExecutable)
        }
        attrs.onDoubleClickFunction = {}
        val checked = fileInfo.isExecutable
        fontAwesomeIcon(icon = faFile, classes = "fa-2x") {
            if (checked) {
                attrs.color = "Green"
            }
        }
        span("fa-layers-text file-extension fa-inverse pl-2 pt-2 small") {
            attrs.onDoubleClickFunction = {}
            attrs["data-fa-transform"] = "down-3 shrink-12.5"
            if (checked) {
                +"exe"
            } else {
                +"file"
            }
        }
    }
}
