/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.save.domain.*
import com.saveourtool.save.frontend.components.basic.codeeditor.FileType
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.http.postUploadFile
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler

import csstype.ClassName
import csstype.Width
import js.core.asList
import js.core.jso
import org.w3c.fetch.Response
import react.*
import react.dom.aria.AriaRole
import web.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.ul
import web.file.File
import web.html.InputType

import kotlinx.browser.window

/**
 * Props for file uploader
 */
external interface HeavyFileUploaderProps<F : Any> : PropsWithChildren {
    /**
     * List of currently selected files.
     */
    var selectedFiles: List<F>

    /**
     * Callback to get name from [F]
     */
    var getName: (F) -> String

    /**
     * Callback to get sizeBytes from [F]
     */
    var getSizeBytes: (F) -> Long

    /**
     * Callback to add file to [selectedFiles]
     */
    var addSelectedFile: (F) -> Unit

    /**
     * Callback to remove file from [selectedFiles]
     */
    var removeSelectedFile: (F) -> Unit

    /**
     * Url for fetching existing in storage files
     */
    var getUrlForAvailableFilesFetch: () -> String

    /**
     * Callback to get url for file uploading to storage
     */
    var getUrlForFileUpload: () -> String

    /**
     * Callback to get url for file downloading from storage
     */
    var getUrlForFileDownload: (F) -> String

    /**
     * Callback to get url for file deletion from storage
     */
    var getUrlForFileDeletion: (F) -> String

    /**
     * Transform [F] to String suitable for displaying
     */
    var fileInfoToPrettyPrint: (F) -> String

    /**
     * Callback to decode [Response] into [F]
     */
    var decodeFileInfoFromString: suspend (Response) -> F

    /**
     * Callback to decode [Response] into [List] of [F]
     */
    var decodeListOfFileInfosFromString: suspend (Response) -> List<F>

    /**
     * Flag that defines if current component is for Sandbox
     */
    var isSandboxMode: Boolean
}

/**
 * @return functional component for file uploading
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
    "ComplexMethod",
)
internal fun <F : Any> heavyFileUploader() = FC<HeavyFileUploaderProps<F>> { props ->
    val (availableFiles, setAvailableFiles) = useState<List<F>>(emptyList())

    val (bytesReceived, setBytesReceived) = useState(0L)
    val (bytesTotal, setBytesTotal) = useState(0L)

    useRequest {
        val listOfFileInfos = get(
            props.getUrlForAvailableFilesFetch(),
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                props.decodeListOfFileInfosFromString(it)
            }
        if (props.isSandboxMode) {
            listOfFileInfos.forEach {
                props.addSelectedFile(it)
            }
        } else {
            setAvailableFiles(listOfFileInfos)
        }
    }

    val (fileToDelete, setFileToDelete) = useState<F?>(null)
    val deleteFile = useDeferredRequest {
        fileToDelete?.let {
            val response = delete(
                props.getUrlForFileDeletion(fileToDelete),
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
            )

            if (response.ok) {
                props.removeSelectedFile(fileToDelete)
                setBytesReceived(bytesReceived - props.getSizeBytes(fileToDelete))
                setBytesTotal(bytesTotal - props.getSizeBytes(fileToDelete))
                setFileToDelete(null)
            }
        }
    }

    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    val uploadFiles = useDeferredRequest {
        filesForUploading.forEach { fileForUploading ->
            if (!props.isSandboxMode || fileForUploading.name != FileType.SETUP_SH.fileName) {
                val response = postUploadFile(
                    url = props.getUrlForFileUpload(),
                    file = fileForUploading,
                    loadingHandler = if (props.isSandboxMode) ::loadingHandler else ::noopLoadingHandler,
                )
                    .let {
                        props.decodeFileInfoFromString(it)
                    }

                setBytesReceived { it + props.getSizeBytes(response) }
                props.addSelectedFile(response)
            } else {
                window.alert("Use code editor instead of file uploader to manage ${fileForUploading.name}, please.")
            }
        }
    }

    div {
        ul {
            className = ClassName("list-group")

            // ===== SELECTED FILES =====
            props.selectedFiles
                .filter { !props.isSandboxMode || props.getName(it) != FileType.SETUP_SH.fileName }
                .map { file ->
                    li {
                        className = ClassName("list-group-item")
                        if (!props.isSandboxMode) {
                            button {
                                type = ButtonType.button
                                className = ClassName("btn")
                                fontAwesomeIcon(icon = faTimesCircle)
                                onClick = {
                                    props.removeSelectedFile(file)
                                    setAvailableFiles(availableFiles + file)
                                }
                            }
                        }
                        a {
                            button {
                                type = ButtonType.button
                                className = ClassName("btn")
                                fontAwesomeIcon(icon = faDownload)
                            }
                            download = props.getName(file)
                            href = props.getUrlForFileDownload(file)
                        }
                        button {
                            type = ButtonType.button
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faTrash)
                            onClick = {
                                val confirm = window.confirm(
                                    "Are you sure you want to delete ${props.getName(file)} file?"
                                )
                                if (confirm) {
                                    setFileToDelete(file)
                                    deleteFile()
                                }
                            }
                        }

                        +props.fileInfoToPrettyPrint(file)
                    }
                }

            // ===== SELECTOR =====
            if (!props.isSandboxMode) {
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

                        availableFiles.map(props.fileInfoToPrettyPrint)
                            .sortedDescending()
                            .map {
                                option {
                                    className = ClassName("list-group-item")
                                    value = it
                                    +it
                                }
                            }
                        onChange = { event ->
                            val availableFile = availableFiles.first {
                                props.fileInfoToPrettyPrint(it) == event.target.value
                            }
                            props.addSelectedFile(availableFile)
                            setAvailableFiles(availableFiles - availableFile)
                            setBytesReceived(bytesReceived + props.getSizeBytes(availableFile))
                            setBytesTotal(bytesTotal + props.getSizeBytes(availableFile))
                        }
                    }
                }
            }

            // ===== UPLOAD FILES BUTTON =====
            li {
                className = ClassName("list-group-item d-flex justify-content-between align-items-center")
                label {
                    className = ClassName("btn btn-outline-secondary m-0")
                    input {
                        type = InputType.file
                        multiple = !props.isSandboxMode
                        hidden = true
                        onChange = { event ->
                            event.target.files!!.asList()
                                .also { filesToUpload ->
                                    setBytesTotal(filesToUpload.sumOf { it.size.toLong() })
                                }
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

            if (!props.isSandboxMode && (bytesReceived < bytesTotal)) {
                div {
                    className = ClassName("progress")
                    div {
                        className = ClassName("progress-bar progress-bar-striped progress-bar-animated")
                        role = "progressbar".unsafeCast<AriaRole>()
                        style = jso {
                            width = if (bytesTotal != 0L) {
                                "${ (100 * bytesReceived / bytesTotal) }%"
                            } else {
                                "100%"
                            }.unsafeCast<Width>()
                        }
                        +"${ bytesReceived / 1024 } / ${ bytesTotal / 1024 } kb"
                    }
                }
            }
        }
    }
    useTooltip()
}
