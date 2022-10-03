/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.v1

import csstype.ClassName
import csstype.Width
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.*
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.files.File
import org.w3c.xhr.FormData

/**
 * Component used to upload file
 */
val fileUploader = fileUploader()

/**
 * A component for file icon that changes depending on executable flag
 */
@Suppress("TYPE_ALIAS", "EMPTY_BLOCK_STRUCTURE_ERROR")
internal val fileIconWithMode: FC<FileIconProps> = FC { props ->
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

/**
 * Props for file uploader
 */
external interface UploaderProps : PropsWithChildren {
    /**
     * Header of the card
     */
    var header: String

    /**
     * Organization and project names
     */
    var projectCoordinates: ProjectCoordinates?

    var selectedFiles: List<FileInfo>

    var setSelectedFiles: (List<FileInfo>) -> Unit
}

/**
 * [Props] for [fileIconWithMode] component
 */
external interface FileIconProps : Props {
    /**
     * [FileInfo] to base the icon on
     */
    var fileInfo: FileInfo

    /**
     * a handler that is invoked when icon is clicked
     */
    @Suppress("TYPE_ALIAS")
    var onExecutableChange: (file: FileInfo, checked: Boolean) -> Unit
}

private fun FileKey.getHref() =
        "/api/$v1/files/${projectCoordinates.organizationName}/${projectCoordinates.projectName}/download?name=$name&uploadedMillis=$uploadedMillis"

@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
)
private fun fileUploader() = FC<UploaderProps> { props ->
    val (bytesReceived, setBytesReceived) = useState(0L)
    val (suiteByteSize, setSuiteByteSize) = useState(0L)
    val (availableFiles, setAvailableFiles) = useState<List<FileInfo>>(emptyList())
    useRequest {
        get(
            "$apiUrl/files/${props.projectCoordinates}/list",
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<FileInfo>>()
            }
            .also {
                setAvailableFiles(it)
            }
    }

    val (fileToDelete, setFileToDelete) = useState<FileInfo?>(null)
    val deleteFile = useDeferredRequest {
        fileToDelete?.let {
            val response = delete(
                with(fileToDelete.key) {
                    "${apiUrl}/files/$projectCoordinates/delete?name=$name&uploadedMillis=$uploadedMillis"
                },
                jsonHeaders,
                Json.encodeToString(fileToDelete),
                loadingHandler = ::noopLoadingHandler,
            )

            if (response.ok) {
                props.setSelectedFiles(props.selectedFiles - fileToDelete)
                setBytesReceived(bytesReceived - fileToDelete.sizeBytes)
                setSuiteByteSize(suiteByteSize - fileToDelete.sizeBytes)
                setFileToDelete(null)
            }
        }
    }

    val (isUploading, setIsUploading) = useState(false)
    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())
    val uploadFiles = useDeferredRequest {
        setIsUploading(true)
        filesForUploading.forEach { file ->
            setSuiteByteSize(suiteByteSize + file.size.toLong())
        }

        filesForUploading.forEach { file ->
            val response: FileInfo = post(
                "$apiUrl/files/${props.projectCoordinates}/upload",
                Headers(),
                FormData().apply {
                    append("file", file)
                },
                loadingHandler = ::noopLoadingHandler,
            )
                .decodeFromJsonString()

                // add only to selected files so that this entry isn't duplicated
            props.setSelectedFiles(props.selectedFiles + response)
            setBytesReceived(bytesReceived + response.sizeBytes)
        }

        setIsUploading(false)
    }

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
                props.selectedFiles.map { fileInfo ->
                    li {
                        className = ClassName("list-group-item")
                        button {
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faTimesCircle)
                            onClick = {
                                props.setSelectedFiles(props.selectedFiles - fileInfo)
                                setAvailableFiles(availableFiles + fileInfo)
                                setBytesReceived(bytesReceived - fileInfo.sizeBytes)
                                setSuiteByteSize(suiteByteSize - fileInfo.sizeBytes)
                            }
                        }
                        a {
                            button {
                                className = ClassName("btn")
                                fontAwesomeIcon(icon = faDownload)
                            }
                            download = fileInfo.key.name
                            href = fileInfo.key.getHref()
                        }
                        button {
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faTrash)
                            onClick = {
                                val confirm = window.confirm(
                                    "Are you sure you want to delete ${fileInfo.key.name} file?"
                                )
                                if (confirm) {
                                    setFileToDelete(fileInfo)
                                    deleteFile()
                                }
                            }
                        }
                        fileIconWithMode {
                            this.fileInfo = fileInfo
                            this.onExecutableChange = { selectedFile, checked ->
                                setAvailableFiles { oldAvailableFiles ->
                                    oldAvailableFiles.apply {
                                        toMutableList().add(
                                            availableFiles.indexOf(selectedFile),
                                            selectedFile.copy(isExecutable = checked),
                                        )
                                    }
                                }
                            }
                        }
                        +fileInfo.toPrettyString()
                    }
                }
                li {
                    className = ClassName("list-group-item d-flex justify-content-between align-items-center")
                    select {
                        className = ClassName("form-control custom-select")
                        value = "default"
                        option {
                            value = "default"
                            disabled = true
                            +"Select a file from existing"
                        }
                        availableFiles.sortedByDescending { it.key.uploadedMillis }.map {
                            option {
                                className = ClassName("list-group-item")
                                value = it.key.name
                                +it.toPrettyString()
                            }
                        }
                        onChange = { event ->
                            val availableFile = availableFiles.first { it.key.name == event.target.value }
                            props.setSelectedFiles(props.selectedFiles + availableFile)
                            setBytesReceived(bytesReceived + availableFile.sizeBytes)
                            setSuiteByteSize(suiteByteSize + availableFile.sizeBytes)
                            setAvailableFiles(availableFiles - availableFile)
                        }
                    }
                }
                li {
                    className = ClassName("list-group-item d-flex justify-content-between align-items-center")
                    label {
                        className = ClassName("btn btn-outline-secondary m-0")
                        input {
                            type = InputType.file
                            multiple = true
                            hidden = true
                            onChange = {
                                setFilesForUploading(it.target.files!!.asList())
                                uploadFiles()
                            }
                        }
                        fontAwesomeIcon(icon = faUpload)
                        asDynamic()["data-toggle"] = "tooltip"
                        asDynamic()["data-placement"] = "top"
                        title = "Regular files/Executable files/ZIP Archives"
                        strong { +" Upload files:" }
                    }
                }

                div {
                    className = ClassName("progress")
                    hidden = !isUploading
                    div {
                        className = ClassName("progress-bar progress-bar-striped progress-bar-animated")
                        style = jso {
                            width = if (suiteByteSize != 0L) {
                                "${ (100 * bytesReceived / suiteByteSize) }%"
                            } else {
                                "100%"
                            }.unsafeCast<Width>()
                        }
                        +"${bytesReceived / 1024} / ${suiteByteSize / 1024} kb"
                    }
                }
            }
        }
    }
    useTooltip()
}
