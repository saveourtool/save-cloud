/**
 * Component for uploading files (FileDtos)
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.http.postUploadFile
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler

import csstype.ClassName
import js.core.asList
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.strong
import react.dom.html.ReactHTML.ul
import web.file.File
import web.html.InputType

@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
    "ComplexMethod",
)
val simpleFileUploader: FC<SimpleFileUploaderProps> = FC { props ->
    val (selectedFiles, setSelectedFiles) = useState<List<FileDto>>(emptyList())
    val (availableFiles, setAvailableFiles) = useState<List<FileDto>>(emptyList())

    useEffect(selectedFiles) { props.updateFileDtos { selectedFiles } }

    useRequest {
        val response = get(
            props.getUrlForDemoFilesFetch(),
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        if (response.ok) {
            setSelectedFiles(response.decodeFromJsonString<List<FileDto>>())
        }
    }

    useRequest(arrayOf(selectedFiles)) {
        props.getUrlForAvailableFilesFetch?.invoke()?.let { url ->
            val response = get(
                url,
                jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
            if (response.ok) {
                val presentNames = selectedFiles.map { it.name }
                response.decodeFromJsonString<List<FileDto>>()
                    .let { fileDtos -> fileDtos.filter { fileDto -> fileDto.name !in presentNames }.distinctBy { it.name } }
                    .let(setAvailableFiles::invoke)
            }
        }
    }

    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    val uploadFiles = useDeferredRequest {
        filesForUploading.forEach { fileForUploading ->
            postUploadFile(
                props.getUrlForFileUpload(),
                fileForUploading,
                loadingHandler = ::noopLoadingHandler,
            )
                .decodeFromJsonString<FileDto>()
                .let { fileDto ->
                    setSelectedFiles { files ->
                        files.plus(fileDto)
                    }
                    props.updateFileDtos { fileDtos ->
                        fileDtos.plus(fileDto)
                    }
                }
        }
    }

    div {
        ul {
            className = ClassName("list-group")

            // ===== SELECTOR =====
            li {
                className = ClassName("list-group-item d-flex justify-content-between align-items-center")
                selectorBuilder(
                    "Select a file from existing",
                    availableFiles.map { it.name }.plus("Select a file from existing"),
                    classes = "form-control custom-select",
                    isDisabled = props.isDisabled,
                ) { event ->
                    val availableFile = availableFiles.first {
                        it.name == event.target.value
                    }
                    setSelectedFiles { it.plus(availableFile) }
                    setAvailableFiles { it.minus(availableFile) }
                }
            }

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
                    title = props.uploadFilesButtonTooltip ?: "Regular files/Executable files/ZIP Archives"
                    asDynamic()["data-toggle"] = "tooltip"
                    asDynamic()["data-placement"] = "top"
                    asDynamic()["data-original-title"] = title
                    strong { +props.buttonLabel }
                }
            }

            // ===== SELECTED FILES =====
            selectedFiles.map { file ->
                li {
                    className = ClassName("list-group-item")
                    buttonBuilder(faTrash, null, isDisabled = props.isDisabled) {
                        setSelectedFiles { it.minus(file) }
                        setAvailableFiles { files -> files.plus(file) }
                    }
                    +file.name
                }
            }
        }
    }
    useTooltip()
}

typealias FileDtosSetter = ((List<FileDto>) -> List<FileDto>) -> Unit

/**
 * Props for simpleFileUploader
 */
external interface SimpleFileUploaderProps : Props {
    /**
     * Callback to get url to get available files
     */
    var getUrlForAvailableFilesFetch: (() -> String)?

    /**
     * Callback to get url to get files that are already present in demo
     */
    var getUrlForDemoFilesFetch: () -> String

    /**
     * Callback to delete file
     */
    var getUrlForFileDeletion: (FileDto) -> String

    /**
     * Callback to get url to upload file
     */
    var getUrlForFileUpload: () -> String

    /**
     * Callback to update list of selected file ids
     */
    var updateFileDtos: FileDtosSetter

    /**
     * Flag that defines if the upload button is disabled
     */
    var isDisabled: Boolean

    /**
     * Upload button label
     */
    var buttonLabel: String

    /**
     * Message that should be displayed as a tooltip of "Upload files" button, defaults to "Regular files/Executable files/ZIP Archives"
     */
    var uploadFilesButtonTooltip: String?
}
