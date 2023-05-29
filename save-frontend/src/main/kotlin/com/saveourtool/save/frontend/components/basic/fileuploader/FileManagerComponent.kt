/**
 * Component for managing files on project level
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.http.postUploadFile
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler

import js.core.asList
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName
import web.file.File
import web.html.ButtonType
import web.html.InputType

import kotlinx.browser.window

/**
 * @return functional component for file uploading
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
    "ComplexMethod",
)
val fileManagerComponent: FC<FileManagerProps> = FC { props ->
    useTooltip()
    val (availableFiles, setAvailableFiles) = useState<List<FileDto>>(emptyList())
    val (storageBytes, setStorageBytes) = useState(0L)

    val (uploadBytesReceived, setUploadBytesReceived) = useState(0L)
    val (uploadBytesTotal, setUploadBytesTotal) = useState(0L)

    useRequest {
        val fileDtos: List<FileDto> = get(
            "$apiUrl/files/${props.projectCoordinates}/list",
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap { it.decodeFromJsonString() }
        setAvailableFiles(fileDtos)
    }

    useEffect(availableFiles) { setStorageBytes(availableFiles.sumOf { it.sizeBytes }) }

    val (fileToDelete, setFileToDelete) = useState<FileDto?>(null)
    val deleteFile = useDeferredRequest {
        fileToDelete?.let { file ->
            val response = delete(
                "$apiUrl/files/delete?fileId=${file.requiredId()}",
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
            )

            if (response.ok) {
                setAvailableFiles { it.minus(file) }
                setFileToDelete(null)
            }
        }
    }

    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    val uploadFiles = useDeferredRequest {
        filesForUploading.forEach { fileForUploading ->
            val uploadedFile: FileDto = postUploadFile(
                url = "$apiUrl/files/${props.projectCoordinates}/upload",
                file = fileForUploading,
                loadingHandler = ::noopLoadingHandler,
            )
                .decodeFromJsonString()
            setUploadBytesReceived { it.plus(uploadedFile.sizeBytes) }
            setAvailableFiles { it.plus(uploadedFile) }
        }
    }

    div {
        ul {
            className = ClassName("list-group")
            @Suppress("MAGIC_NUMBER", "MagicNumber")
            li {
                val storageSizeMegabytes = storageBytes.toMegabytes()
                // todo: storage size shouldn't be more then 500MB per project
                val textColor = when {
                    storageSizeMegabytes > 450 -> "text-danger"
                    storageSizeMegabytes > 350 -> "text-warning"
                    else -> "text-dark"
                }
                className = ClassName("list-group-item d-flex justify-content-center $textColor")
                +"Your storage is filled with ${storageSizeMegabytes.toFixedStr(2)} MB / 500MB."
            }
            // ===== SELECTED FILES =====
            availableFiles.map { file ->
                li {
                    className = ClassName("list-group-item")
                    a {
                        button {
                            type = ButtonType.button
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faDownload)
                        }
                        download = file.name
                        href = "$apiUrl/files/download?fileId=${file.requiredId()}"
                    }
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

                    +file.prettyPrint()
                }
            }
            // ===== UPLOAD FILES BUTTON =====
            li {
                className = ClassName("list-group-item d-flex justify-content-center align-items-center")
                label {
                    className = ClassName("btn btn-outline-secondary m-0")
                    input {
                        type = InputType.file
                        multiple = true
                        hidden = true
                        onChange = { event ->
                            event.target.files!!.asList()
                                .also { files -> setUploadBytesTotal(files.sumOf { it.size }.toLong()) }
                                .let { setFilesForUploading(it) }
                            uploadFiles()
                        }
                    }
                    fontAwesomeIcon(icon = faUpload)
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "top"
                    title = "Regular files/Executable files/ZIP Archives"
                    strong { +" Upload files " }
                }
            }
            progressBarComponent {
                current = uploadBytesReceived
                total = uploadBytesTotal
                getLabelText = { current, total -> "${current.toKiloBytes()} / ${total.toKiloBytes()} KB" }
                flushCounters = {
                    setUploadBytesTotal(0)
                    setUploadBytesReceived(0)
                }
            }
        }
    }
}

/**
 * Props for [fileManagerComponent]
 */
external interface FileManagerProps : Props {
    /**
     * [ProjectCoordinates] of current project
     */
    var projectCoordinates: ProjectCoordinates
}

@Suppress("MagicNumber", "MAGIC_NUMBER")
private fun Long.toKiloBytes() = div(1024)

@Suppress("MagicNumber", "MAGIC_NUMBER")
private fun Long.toMegabytes() = toDouble().div(1024 * 1024)
