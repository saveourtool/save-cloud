/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.save.domain.*
import com.saveourtool.save.frontend.common.components.basic.fileuploader.deleteFileButton
import com.saveourtool.save.frontend.common.components.basic.fileuploader.downloadFileButton
import com.saveourtool.save.frontend.common.utils.*
import com.saveourtool.save.frontend.common.utils.noopLoadingHandler
import com.saveourtool.save.frontend.components.basic.codeeditor.FileType
import com.saveourtool.save.frontend.common.components.inputform.dragAndDropForm
import com.saveourtool.save.frontend.http.postUploadFile

import js.core.asList
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName
import web.file.File

import kotlinx.browser.window

/**
 * [FC] for sandbox file uploading
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TYPE_ALIAS",
    "LongMethod",
    "ComplexMethod",
)
val sandboxFileUploader: FC<SandboxFileUploaderProps> = FC { props ->
    val (selectedFiles, setSelectedFiles) = useState<List<SandboxFileInfo>>(emptyList())

    useRequest {
        val listOfFileInfos: List<SandboxFileInfo> = get(
            props.getUrlForAvailableFilesFetch(),
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        ).decodeFromJsonString()
        setSelectedFiles { it + listOfFileInfos }
    }

    val (fileToDelete, setFileToDelete) = useState<SandboxFileInfo?>(null)
    val deleteFile = useDeferredRequest {
        fileToDelete?.let {
            val response = delete(
                props.getUrlForFileDeletion(fileToDelete),
                jsonHeaders,
                loadingHandler = ::loadingHandler,
            )

            if (response.ok) {
                setSelectedFiles { it - fileToDelete }
                setFileToDelete(null)
            }
        }
    }

    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())
    val uploadFiles = useDeferredRequest {
        filesForUploading.forEach { fileForUploading ->
            if (fileForUploading.name != FileType.SETUP_SH.fileName) {
                val uploadedFileInfo: SandboxFileInfo = postUploadFile(
                    url = props.getUrlForFileUpload(),
                    file = fileForUploading,
                    loadingHandler = ::loadingHandler,
                )
                    .decodeFromJsonString()
                setSelectedFiles { it + uploadedFileInfo }
            } else {
                window.alert("Use code editor instead of file uploader to manage ${fileForUploading.name}, please.")
            }
        }
    }

    div {
        ul {
            className = ClassName("list-group")

            // ===== SELECTED FILES =====
            selectedFiles
                .filter { it.name != FileType.SETUP_SH.fileName }
                .map { file ->
                    li {
                        className = ClassName("list-group-item")
                        downloadFileButton(file, SandboxFileInfo::name, props.getUrlForFileDownload)
                        deleteFileButton(file, SandboxFileInfo::name) {
                            setFileToDelete(it)
                            deleteFile()
                        }
                        +file.name
                    }
                }

            // ===== UPLOAD FILES BUTTON =====
            li {
                className = ClassName("list-group-item p-0 d-flex bg-light")
                dragAndDropForm {
                    isDisabled = false
                    isMultipleFilesSupported = false
                    tooltipMessage = "upload your tested tool and all other needed files"
                    onChangeEventHandler = { files ->
                        setFilesForUploading(files!!.asList())
                        uploadFiles()
                    }
                }
            }
        }
    }
    useTooltip()
}

/**
 * Props for file uploader
 */
external interface SandboxFileUploaderProps : Props {
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
    var getUrlForFileDownload: (SandboxFileInfo) -> String

    /**
     * Callback to get url for file deletion from storage
     */
    var getUrlForFileDeletion: (SandboxFileInfo) -> String
}
