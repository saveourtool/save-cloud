/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.*
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler

import csstype.ClassName
import csstype.Width
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.files.File
import org.w3c.xhr.FormData
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

import kotlinx.browser.window
import kotlinx.js.jso

val fileUploader = fileUploader()

/**
 * A component for file icon that changes depending on executable flag
 */
@Suppress("TYPE_ALIAS", "EMPTY_BLOCK_STRUCTURE_ERROR")
internal val fileIconWithMode: FC<FileIconProps> = FC { props ->
    if (props.fileInfo is FileInfo) {
        val fileInfo = props.fileInfo as FileInfo
        span {
            className = ClassName("fa-layers mr-3")
            title = "Click to mark file ${if (fileInfo.isExecutable) "regular" else "executable"}"
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "top"
            // if file was not executable, after click it will be; and vice versa
            onClick = { _ ->
                // hide previous tooltip, otherwise it gets stuck during re-render
                val jquery = kotlinext.js.require("jquery")
                jquery("[data-toggle=\"tooltip\"]").tooltip("hide")
                props.onExecutableChange(!fileInfo.isExecutable)
            }
            onDoubleClick = {}
            val checked = fileInfo.isExecutable
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
    } else {
        fontAwesomeIcon(icon = faFile, classes = "fa-2x")
    }
}

/**
 * Props for file uploader
 */
external interface UploaderProps : PropsWithChildren {
    /**
     * List of currently selected files.
     */
    var selectedFiles: List<AbstractFileInfo>

    /**
     * Callback to update [selectedFiles]
     */
    var setSelectedFiles: (List<AbstractFileInfo>) -> Unit

    /**
     * Url for fetching existing in storage files
     */
    var urlForAvailableFilesFetch: String?

    /**
     * Callback to get url for file uploading to storage
     */
    var getUrlForFileUpload: () -> String

    /**
     * Callback to get url for file downloading from storage
     */
    var getUrlForFileDownload: (AbstractFileInfo) -> String

    /**
     * Callback to get url for file deletion from storage
     */
    var getUrlForFileDeletion: (AbstractFileInfo) -> String

    /**
     * Transform [AbstractFileInfo] to String suitable for displaying
     */
    var fileInfoToPrettyPrint: (AbstractFileInfo) -> String
}

/**
 * [Props] for [fileIconWithMode] component
 */
external interface FileIconProps : Props {
    /**
     * [FileInfo] to base the icon on
     */
    var fileInfo: AbstractFileInfo

    /**
     * a handler that is invoked when icon is clicked
     */
    @Suppress("TYPE_ALIAS")
    var onExecutableChange: (checked: Boolean) -> Unit
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
    "ComplexMethod",
)
private fun fileUploader() = FC<UploaderProps> { props ->
    val (bytesReceived, setBytesReceived) = useState(0L)
    val (suiteByteSize, setSuiteByteSize) = useState(0L)
    val (availableFiles, setAvailableFiles) = useState<List<AbstractFileInfo>>(emptyList())
    useRequest {
        props.urlForAvailableFilesFetch?.let { urlForAvailableFilesFetch ->
            get(
                urlForAvailableFilesFetch,
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
    }

    val (fileToDelete, setFileToDelete) = useState<AbstractFileInfo?>(null)
    val deleteFile = useDeferredRequest {
        fileToDelete?.let {
            val response = delete(
                props.getUrlForFileDeletion(fileToDelete),
                jsonHeaders,
                undefined,
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
                props.getUrlForFileUpload(),
                Headers(),
                FormData().apply {
                    append("file", file)
                },
                loadingHandler = ::noopLoadingHandler,
            )
                .decodeFromJsonString()

            props.setSelectedFiles(props.selectedFiles + response)
            setBytesReceived(bytesReceived + response.sizeBytes)
        }

        setIsUploading(false)
    }

    div {
        ul {
            className = ClassName("list-group")
            props.selectedFiles.map { file ->
                li {
                    className = ClassName("list-group-item")
                    if (file is FileInfo) {
                        button {
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faTimesCircle)
                            onClick = {
                                props.setSelectedFiles(props.selectedFiles - file)
                                setAvailableFiles(availableFiles + file)
                                setBytesReceived(bytesReceived - file.sizeBytes)
                                setSuiteByteSize(suiteByteSize - file.sizeBytes)
                            }
                        }
                    }
                    a {
                        button {
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faDownload)
                        }
                        download = file.name
                        href = props.getUrlForFileDownload(file)
                    }
                    button {
                        className = ClassName("btn")
                        fontAwesomeIcon(icon = faTrash)
                        onClick = {
                            val confirm = window.confirm(
                                "Are you sure you want to delete ${file.name} file?"
                            )
                            if (confirm) {
                                setFileToDelete(file)
                                deleteFile()
                            }
                        }
                    }
                    fileIconWithMode {
                        this.fileInfo = file
                        this.onExecutableChange = { checked ->
                            if (file is FileInfo) {
                                val index = props.selectedFiles.indexOf(file)
                                props.selectedFiles.toMutableList()
                                    .apply {
                                        removeAt(index)
                                        add(
                                            index,
                                            file.copy(isExecutable = checked),
                                        )
                                    }
                                    .toList()
                                    .let {
                                        props.setSelectedFiles(it)
                                    }
                            }
                        }
                    }

                    +props.fileInfoToPrettyPrint(file)
                }
            }
            props.urlForAvailableFilesFetch?.let {
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

                        availableFiles.sortedByDescending {
                            if (it is FileInfo) {
                                it.key.uploadedMillis.toString()
                            } else {
                                it.name
                            }
                        }.map {
                            option {
                                className = ClassName("list-group-item")
                                value = if (it is FileInfo) {
                                    "${it.name}-${it.key.uploadedMillis}"
                                } else {
                                    it.name
                                }
                                +props.fileInfoToPrettyPrint(it)
                            }
                        }
                        onChange = { event ->
                            val availableFile = availableFiles.first { it.name == event.target.value }
                            props.setSelectedFiles(props.selectedFiles + availableFile)
                            setBytesReceived(bytesReceived + availableFile.sizeBytes)
                            setSuiteByteSize(suiteByteSize + availableFile.sizeBytes)
                            setAvailableFiles(availableFiles - availableFile)
                        }
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
    useTooltip()
}
