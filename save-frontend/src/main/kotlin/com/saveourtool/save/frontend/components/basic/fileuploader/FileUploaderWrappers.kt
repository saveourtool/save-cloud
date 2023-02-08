/**
 * Component for uploading files
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.frontend.components.views.sandboxApiUrl
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*

import io.ktor.http.escapeIfNeeded
import react.*

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val fileUploaderOverFileInfo = heavyFileUploader<FileDto>()

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val fileUploaderOverSandboxFileInfo = heavyFileUploader<SandboxFileInfo>()

/**
 * @param selectedFilesFromState
 * @param addSelectedFile
 * @param removeSelectedFile
 */
fun ChildrenBuilder.fileUploaderForSandbox(
    selectedFilesFromState: List<SandboxFileInfo>,
    addSelectedFile: (SandboxFileInfo) -> Unit,
    removeSelectedFile: (SandboxFileInfo) -> Unit,
) {
    fileUploaderOverSandboxFileInfo {
        isSandboxMode = true
        getName = { it.name }
        getSizeBytes = { it.sizeBytes }
        selectedFiles = selectedFilesFromState
        getUrlForAvailableFilesFetch = { "$sandboxApiUrl/list-file" }
        getUrlForFileUpload = { "$sandboxApiUrl/upload-file" }
        getUrlForFileDownload = { fileInfo ->
            "$sandboxApiUrl/download-file?fileName=${fileInfo.name.escapeIfNeeded()}"
        }
        getUrlForFileDeletion = { fileInfo ->
            "$sandboxApiUrl/delete-file?fileName=${fileInfo.name.escapeIfNeeded()}"
        }
        fileInfoToPrettyPrint = { it.name }
        decodeFileInfoFromString = {
            it.decodeFromJsonString()
        }
        decodeListOfFileInfosFromString = {
            it.decodeFromJsonString()
        }
        this.addSelectedFile = addSelectedFile
        this.removeSelectedFile = removeSelectedFile
    }
}

/**
 * Shell for fileSelector to use it in ProjectView
 *
 * @param projectCoordinates
 * @param selectedFilesFromState
 * @param addSelectedFile
 * @param removeSelectedFile
 */
fun ChildrenBuilder.fileUploaderForProjectRun(
    projectCoordinates: ProjectCoordinates,
    selectedFilesFromState: List<FileDto>,
    addSelectedFile: (FileDto) -> Unit,
    removeSelectedFile: (FileDto) -> Unit,
) {
    fileUploaderOverFileInfo {
        isSandboxMode = false
        getName = { it.name }
        getSizeBytes = { it.sizeBytes }
        selectedFiles = selectedFilesFromState
        getUrlForAvailableFilesFetch = { "$apiUrl/files/$projectCoordinates/list" }
        getUrlForFileUpload = { "$apiUrl/files/$projectCoordinates/upload" }
        getUrlForFileDownload = { fileInfo ->
            with(fileInfo) {
                "$apiUrl/files/download?fileId=${requiredId()}"
            }
        }
        getUrlForFileDeletion = { fileInfo ->
            with(fileInfo) {
                "$apiUrl/files/delete?fileId=${requiredId()}"
            }
        }
        @Suppress("MAGIC_NUMBER")
        fileInfoToPrettyPrint = {
            "${it.name} (uploaded at ${it.uploadedTime}, size ${it.sizeBytes / 1024} KiB)"
        }
        decodeFileInfoFromString = {
            it.decodeFromJsonString()
        }
        decodeListOfFileInfosFromString = {
            it.decodeFromJsonString()
        }
        this.addSelectedFile = addSelectedFile
        this.removeSelectedFile = removeSelectedFile
    }
}
