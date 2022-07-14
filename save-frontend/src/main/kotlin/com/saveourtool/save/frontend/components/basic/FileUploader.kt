/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.ConfirmationType
import com.saveourtool.save.frontend.utils.toPrettyString
import com.saveourtool.save.v1

import csstype.ClassName
import csstype.Width
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.*
import react.dom.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.ul

import kotlinx.js.jso

/**
 * Component used to upload file
 */
val fileUploader = fileUploader()

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
     * Submit button was pressed
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
    var isUploading: Boolean?

    /**
     * Organization and project names
     */
    var projectCoordinates: ProjectCoordinates?

    /**
     * Callback invoked when a file is selected from [UploaderProps.availableFiles]
     */
    var onFileSelect: (HTMLSelectElement) -> Unit

    /**
     * Callback invoked when a file is removed from selection by pushing a button
     */
    var onFileRemove: (FileInfo) -> Unit

    /**
     * Callback invoked on `input` change events
     */
    var onFileInput: (HTMLInputElement) -> Unit

    /**
     * Callback invoked when a file is deleted forever
     */
    var onFileDelete: (FileInfo) -> Unit

    /**
     * Callback invoked when file is checked to be executable or vice versa
     */
    @Suppress("TYPE_ALIAS")
    var onExecutableChange: (file: FileInfo, checked: Boolean) -> Unit
}

external interface FileIconProps : Props {
    /**
     * [FileInfo] to base the icon on
     */
    var fileInfo: FileInfo

    /**
     * a handler that is invoked when icon is clicked
     */
    var onExecutableChange: (file: FileInfo, checked: Boolean) -> Unit
}

/**
 * A component for file icon that changes depending on executable flag
 */
@Suppress("TYPE_ALIAS", "EMPTY_BLOCK_STRUCTURE_ERROR")
internal val fileIconWithMode = FC<FileIconProps> { props ->
    span {
        className = ClassName("fa-layers mr-3")
        title = "Click to mark file ${if (props.fileInfo.isExecutable) "regular" else "executable"}"
        asDynamic()["data-toggle"] = "tooltip"
        asDynamic()["data-placement"] = "top"
        // if file was not executable, after click it will be; and vice versa
        onClick = { _ ->
            // hide previous tooltip, otherwise it gets stuck during re-render
            val jquery = kotlinext.js.require("jquery")
            jquery("[data-toggle=\"tooltip\"]").tooltip("hide")
            props.onExecutableChange(props.fileInfo, !props.fileInfo.isExecutable)
        }
        onDoubleClick = {}
        val checked = props.fileInfo.isExecutable
        fontAwesomeIcon(icon = faFile, classes = "fa-2x") {
            if (checked) {
                asDynamic()["color"] = "Green"
            }
        }
        span {
            className = ClassName("fa-layers-text file-extension fa-inverse pl-2 pt-2 small")
            onDoubleClick = {}
            asDynamic()["data-fa-transform"] = "down-3 shrink-12.5"
            if (checked) {
                +"exe"
            } else {
                +"file"
            }
        }
    }
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
)
private fun fileUploader() = FC<UploaderProps> { props ->
    div {
        className = ClassName("mb-3")
        div {
            className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
            +props.header
        }

        div {
            label {
                className = ClassName("control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-1 pl-0")
                +"1. Upload or select the tool (and other resources) for testing:"
            }

            ul {
                className = ClassName("list-group")
                props.files.map { fileInfo ->
                    li {
                        className = ClassName("list-group-item")
                        button {
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faTimesCircle)
                            onClick = {
                                props.onFileRemove(fileInfo)
                            }
                        }
                        a {
                            button {
                                className = ClassName("btn")
                                fontAwesomeIcon(icon = faDownload)
                            }
                            download = fileInfo.name
                            href = getHref(fileInfo, props.projectCoordinates)
                        }
                        button {
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faTrash)
                            onClick = {
                                props.onFileDelete(fileInfo)
                            }
                        }
                        fileIconWithMode {
                            this.fileInfo = fileInfo
                            this.onExecutableChange = props.onExecutableChange
                        }
                        +fileInfo.toPrettyString()
                    }
                }
                li {
                    className = ClassName("list-group-item d-flex justify-content-between align-items-center")
                    val wasSubmitted = props.isSubmitButtonPressed ?: false
                    val form = if (props.files.isEmpty() && wasSubmitted && props.confirmationType == ConfirmationType.NO_BINARY_CONFIRM) {
                        "form-control is-invalid"
                    } else {
                        "form-control"
                    }

                    select {
                        className = ClassName(form)
                        value = "default"
                        option {
                            value = "default"
                            disabled = true
                            +"Select a file from existing"
                        }
                        props.availableFiles.sortedByDescending { it.uploadedMillis }.map {
                            option {
                                className = ClassName("list-group-item")
                                value = it.name
                                +it.toPrettyString()
                            }
                        }
                        onChange = {
                            props.onFileSelect(it.target)
                        }
                    }
                }
                li {
                    className = ClassName("list-group-item d-flex justify-content-between align-items-center")
                    label {
                        input {
                            type = InputType.file
                            multiple = true
                            hidden = true
                            onChange = {
                                props.onFileInput(it.target)
                            }
                        }
                        fontAwesomeIcon(icon = faUpload)
                        asDynamic()["data-toggle"] = "tooltip"
                        asDynamic()["data-placement"] = "top"
                        title = "Regular files/Executable files/ZIP Archives"
                        strong { +"Upload files:" }
                    }
                }

                div {
                    className = ClassName("progress")
                    hidden = !(props.isUploading ?: false)
                    div {
                        className = ClassName("progress-bar progress-bar-striped progress-bar-animated")
                        style = jso {
                            width = if (props.suiteByteSize != 0L) {
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

    useEffect {
        val jquery = kotlinext.js.require("jquery")
        kotlinext.js.require("popper.js")
        kotlinext.js.require("bootstrap")
        jquery("[data-toggle=\"tooltip\"]").tooltip()
    }
}

private fun getHref(
    fileInfo: FileInfo,
    projectCoordinates: ProjectCoordinates?
) =
        "/api/$v1/resource/${projectCoordinates?.organizationName}/${projectCoordinates?.projectName}/${fileInfo.uploadedMillis}/${fileInfo.name}"
