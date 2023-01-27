/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler

import csstype.ClassName
import js.core.asList
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.ul
import web.file.File
import web.http.FormData

import kotlinx.browser.window

@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
    "ComplexMethod",
)
val simpleFileUploader: FC<SimpleFileUploaderProps> = FC { props ->
    val (selectedFiles, setSelectedFiles) = useState<List<FileDto>>(emptyList())
    useRequest {
        val response = get(
            props.getUrlForAvailableFilesFetch(),
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        if (response.ok) {
            setSelectedFiles(response.decodeFromJsonString<List<FileDto>>())
        }
    }

    val (fileToDelete, setFileToDelete) = useState<FileDto?>(null)
    val deleteFile = useDeferredRequest {
        fileToDelete?.let {
            val response = delete(
                props.getUrlForFileDeletion(fileToDelete),
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
            )

            if (response.ok) {
                setSelectedFiles {
                    it.minus(fileToDelete)
                }
                setFileToDelete(null)
            }
        }
    }

    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    val uploadFiles = useDeferredRequest {
        filesForUploading.forEach { fileForUploading ->
            post(
                props.getUrlForFileUpload(),
                Headers(),
                FormData().apply {
                    append("file", fileForUploading)
                },
                loadingHandler = ::noopLoadingHandler,
            )
                .decodeFromJsonString<FileDto>()
                .let { fileDto ->
                    setSelectedFiles { files ->
                        files.plus(fileDto)
                    }
                }
        }
    }

    div {
        ul {
            className = ClassName("list-group")

            // ===== UPLOAD FILES BUTTON =====
            li {
                className = ClassName("list-group-item d-flex justify-content-between align-items-center")
                label {
                    val disable = if (props.isDisabled) "disabled" else ""
                    className = ClassName("btn btn-outline-secondary m-0 $disable")
                    input {
                        type = InputType.file
                        disabled = props.isDisabled
                        multiple = true
                        hidden = true
                        onChange = { event ->
                            setFilesForUploading(event.target.files!!.asList())
                            uploadFiles()
                        }
                    }
                    fontAwesomeIcon(icon = faUpload)
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "top"
                    title = "Regular files/Executable files/ZIP Archives"
                    strong { +props.buttonLabel }
                }
            }

            // ===== SELECTED FILES =====
            selectedFiles.map { file ->
                li {
                    className = ClassName("list-group-item")
                    button {
                        type = ButtonType.button
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
                    +file.name
                }
            }
        }
    }
    useTooltip()
}

/**
 * Props for simpleFileUploader
 */
external interface SimpleFileUploaderProps : Props {
    /**
     * Callback to get url to get available files
     */
    var getUrlForAvailableFilesFetch: () -> String

    /**
     * Callback to delete file
     */
    var getUrlForFileDeletion: (FileDto) -> String

    /**
     * Callback to get url to upload file
     */
    var getUrlForFileUpload: () -> String

    /**
     * Flag that defines if the upload button is disabled
     */
    var isDisabled: Boolean

    /**
     * Upload button label
     */
    var buttonLabel: String
}
