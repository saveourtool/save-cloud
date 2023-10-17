@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.entities.cosv.UnzipRawCosvFileResponse
import com.saveourtool.save.frontend.components.basic.selectFormRequired
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.dragAndDropForm
import com.saveourtool.save.frontend.externals.fontawesome.faBoxOpen
import com.saveourtool.save.frontend.externals.fontawesome.faReload
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.i18next.useTranslation
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.ARCHIVE_EXTENSION
import com.saveourtool.save.utils.FILE_PART_NAME
import com.saveourtool.save.validation.isValidName

import js.core.asList
import js.core.jso
import org.w3c.fetch.Headers
import react.FC
import react.Props
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul
import react.useState
import web.cssom.ClassName
import web.cssom.Cursor
import web.file.File
import web.html.ButtonType
import web.html.InputType
import web.http.FormData

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.serialization.json.Json

private const val DEFAULT_SIZE = 10

val cosvFileManagerComponent: FC<Props> = FC { _ ->
    useTooltip()
    val (t) = useTranslation("vulnerability-upload")

    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    val organizationSelectForm = selectFormRequired<String>()

    val (allAvailableFilesCount, setAllAvailableFilesCount) = useState(0L)
    val (lastPage, setLastPage) = useState(0)
    val (availableFiles, setAvailableFiles) = useState<List<RawCosvFileDto>>(emptyList())
    val (selectedFiles, setSelectedFiles) = useState<List<RawCosvFileDto>>(emptyList())
    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())

    val leftAvailableFilesCount = allAvailableFilesCount - lastPage * DEFAULT_SIZE

    val (userOrganizations, setUserOrganizations) = useState(emptyList<OrganizationDto>())
    val (selectedOrganization, setSelectedOrganization) = useState<String>()

    val (fileToDelete, setFileToDelete) = useState<RawCosvFileDto>()
    val (fileToUnzip, setFileToUnzip) = useState<RawCosvFileDto>()

    val (processedBytes, setProcessedBytes) = useState(0L)
    val (totalBytes, setTotalBytes) = useState(0L)

    val (isStreamingOperationActive, setStreamingOperationActive) = useState(false)

    val deleteFile = useDeferredRequest {
        fileToDelete?.let { file ->
            val response = delete(
                "$apiUrl/raw-cosv/$selectedOrganization/delete/${file.requiredId()}",
                jsonHeaders,
                loadingHandler = ::loadingHandler,
            )

            if (response.ok) {
                setAvailableFiles { it.minus(file) }
                setFileToDelete(null)
            } else {
                window.alert("Failed to delete file due to ${response.unpackMessageOrHttpStatus()}")
            }
        }
    }

    val deleteProcessedFiles = useDeferredRequest {
        val response = delete(
            "$apiUrl/raw-cosv/$selectedOrganization/delete-processed",
            jsonHeaders,
            loadingHandler = ::loadingHandler,
        )

        if (response.ok) {
            val deletedFiles: Set<RawCosvFileDto> = response.decodeFromJsonString()
            setAvailableFiles { it.minus(deletedFiles) }
            setAllAvailableFilesCount { it.minus(deletedFiles.size) }
        } else {
            window.alert("Failed to delete processed files due to ${response.unpackMessageOrHttpStatus()}")
        }
    }

    useRequest {
        val organizations = get(
            url = "$apiUrl/organizations/with-allow-bulk-upload",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<OrganizationDto>>()
            }

        setUserOrganizations(organizations)
    }

    val fetchMoreFiles = useDeferredRequest {
        selectedOrganization?.let {
            val newPage = lastPage.inc()
            val response = get(
                url = "$apiUrl/raw-cosv/$selectedOrganization/list",
                params = jso<dynamic> {
                    page = newPage - 1
                    size = DEFAULT_SIZE
                },
                headers = Headers().withAcceptNdjson().withContentTypeJson(),
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler
            )
            when {
                response.ok -> {
                    setStreamingOperationActive(true)
                    response
                        .readLines()
                        .filter(String::isNotEmpty)
                        .onCompletion {
                            setStreamingOperationActive(false)
                            setLastPage(newPage)
                        }
                        .collect { message ->
                            val uploadedFile: RawCosvFileDto = Json.decodeFromString(message)
                            setAvailableFiles { it.plus(uploadedFile) }
                        }
                }
                else -> window.alert("Failed to fetch next page: ${response.unpackMessageOrNull().orEmpty()}")
            }
        }
    }
    val reFetchFiles = useDeferredRequest {
        selectedOrganization?.let {
            val count: Long = get(
                url = "$apiUrl/raw-cosv/$selectedOrganization/count",
                jsonHeaders,
                loadingHandler = ::loadingHandler,
                responseHandler = ::noopResponseHandler
            ).decodeFromJsonString()
            setAvailableFiles(emptyList())
            setAllAvailableFilesCount(count)
            setLastPage(0)
            fetchMoreFiles()
        }
    }

    val uploadFiles = useDeferredRequest {
        setStreamingOperationActive(true)
        val response = post(
            url = "$apiUrl/raw-cosv/$selectedOrganization/batch-upload",
            headers = Headers().withAcceptNdjson(),
            body = FormData().apply { filesForUploading.forEach { append(FILE_PART_NAME, it) } },
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
        )
        when {
            response.ok -> response
                .readLines()
                .filter(String::isNotEmpty)
                .onCompletion {
                    setStreamingOperationActive(false)
                    reFetchFiles()
                }
                .collect { message ->
                    val uploadedFile: RawCosvFileDto = Json.decodeFromString(message)
                    setProcessedBytes { it.plus(uploadedFile.requiredContentLength()) }
                }
            else -> window.alert(response.unpackMessageOrNull().orEmpty())
        }
    }

    val unzipFile = useDeferredRequest {
        fileToUnzip?.let { file ->
            setStreamingOperationActive(true)
            val response = post(
                "$apiUrl/raw-cosv/$selectedOrganization/unzip/${file.requiredId()}",
                headers = Headers().withContentTypeJson().withAcceptNdjson(),
                body = undefined,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )

            when {
                response.ok -> {
                    setAvailableFiles {
                        it.minus(file)
                    }
                    response
                        .readLines()
                        .filter(String::isNotEmpty)
                        .onCompletion {
                            setStreamingOperationActive(false)
                        }
                        .collect { message ->
                            val entryResponse: UnzipRawCosvFileResponse = Json.decodeFromString(message)
                            if (entryResponse.updateCounters) {
                                setTotalBytes(entryResponse.fullSize)
                                setProcessedBytes(entryResponse.processedSize)
                            } else {
                                setProcessedBytes { it + entryResponse.processedSize }
                            }
                        }
                }
                else -> window.alert("Failed to unzip ${file.fileName}: ${response.unpackMessageOrNull().orEmpty()}")
            }
            if (response.ok) {
                setFileToUnzip(null)
                reFetchFiles()
            }
        }
    }

    val submitCosvFiles = useDeferredRequest {
        val response = post(
            url = "$apiUrl/raw-cosv/$selectedOrganization/submit-to-process",
            jsonHeaders,
            body = selectedFiles.map { it.requiredId() },
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler
        )
        if (response.ok) {
            window.alert(response.text().await())
        }
        setSelectedFiles(emptyList())
    }

    val submitAllUploadedCosvFiles = useDeferredRequest {
        val response = post(
            url = "$apiUrl/raw-cosv/$selectedOrganization/submit-all-uploaded-to-process",
            jsonHeaders,
            body = undefined,
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler
        )
        if (response.ok) {
            window.alert(response.text().await())
        }
        setSelectedFiles(emptyList())
    }

    div {
        if (selectedOrganization.isNullOrEmpty()) {
            div {
                className = ClassName("mx-auto")
                b {
                    +"${"Organization that has permission".t()}!"
                }
            }
        }

        organizationSelectForm {
            selectClasses = "custom-select"
            formType = InputTypes.ORGANIZATION_NAME
            validInput = !selectedOrganization.isNullOrEmpty() && selectedOrganization.isValidName()
            classes = "mb-3"
            formName = "Organization"
            getData = { userOrganizations.map { it.name } }
            dataToString = { it }
            selectedValue = selectedOrganization.orEmpty()
            disabled = false
            onChangeFun = { value ->
                setSelectedOrganization(value)
                reFetchFiles()
            }
        }

        ul {
            className = ClassName("list-group")

            // SUBMIT to process
            li {
                className = ClassName("list-group-item p-0 d-flex bg-light justify-content-center")
                buttonBuilder("Delete all processed", classes = "mr-1", isDisabled = availableFiles.noneWithStatus(RawCosvFileStatus.PROCESSED) || isStreamingOperationActive) {
                    deleteProcessedFiles()
                }
                buttonBuilder("Submit", classes = "mr-1", isDisabled = selectedFiles.isEmpty() || isStreamingOperationActive) {
                    submitCosvFiles()
                }
                buttonBuilder("Submit all uploaded", classes = "mr-1", isDisabled = availableFiles.noneWithStatus(RawCosvFileStatus.UPLOADED) || isStreamingOperationActive) {
                    submitAllUploadedCosvFiles()
                }
                buttonBuilder(faReload, isDisabled = isStreamingOperationActive) {
                    reFetchFiles()
                }
            }

            // ===== UPLOAD FILES BUTTON =====
            li {
                className = ClassName("list-group-item p-0 d-flex bg-light")
                dragAndDropForm {
                    isDisabled = selectedOrganization.isNullOrEmpty() || isStreamingOperationActive
                    isMultipleFilesSupported = true
                    tooltipMessage = "Only JSON files or ZIP archives"
                    onChangeEventHandler = { files ->
                        files!!.asList()
                            .also { setTotalBytes(it.sumOf(File::size).toLong()) }
                            .let { setFilesForUploading(it) }
                        uploadFiles()
                    }
                }
            }
            progressBarComponent {
                current = processedBytes
                total = totalBytes
                flushCounters = {
                    setTotalBytes(0)
                    setProcessedBytes(0)
                }
            }

            // ===== SELECTED FILES =====
            availableFiles.map { file ->
                li {
                    className = ClassName("list-group-item text-left")
                    input {
                        className = ClassName("mx-auto")
                        type = InputType.checkbox
                        id = "checkbox"
                        checked = file in selectedFiles
                        disabled = file.isNotSelectable()
                        onChange = { event ->
                            if (event.target.checked) {
                                setSelectedFiles { it.plus(file) }
                            } else {
                                setSelectedFiles { it.minus(file) }
                            }
                        }
                    }
                    downloadFileButton(file, RawCosvFileDto::fileName) {
                        "$apiUrl/raw-cosv/$selectedOrganization/download/${file.requiredId()}"
                    }
                    if (file.fileName.endsWith(ARCHIVE_EXTENSION, ignoreCase = true)) {
                        button {
                            type = ButtonType.button
                            className = ClassName("btn")
                            fontAwesomeIcon(icon = faBoxOpen)
                            disabled = isStreamingOperationActive
                            onClick = {
                                val confirm = window.confirm(
                                    "Are you sure you want to unzip and then remove ${file.fileName} file?"
                                )
                                if (confirm) {
                                    setFileToUnzip(file)
                                    unzipFile()
                                }
                            }
                        }
                    }
                    deleteFileButton(file, RawCosvFileDto::fileName) {
                        setFileToDelete(it)
                        deleteFile()
                    }

                    +"${file.fileName} "
                    span {
                        style = jso {
                            cursor = "pointer".unsafeCast<Cursor>()
                        }
                        val textColor = if (file.status == RawCosvFileStatus.FAILED) {
                            "text-danger"
                        } else {
                            "text-gray-400"
                        }
                        className = ClassName("$textColor text-justify")
                        file.statusMessage?.let { statusMessage ->
                            onClick = {
                                window.alert(statusMessage)
                            }
                        }
                        +when (file.status) {
                            RawCosvFileStatus.IN_PROGRESS -> " (in progress)"
                            RawCosvFileStatus.PROCESSED -> " (processed, will be deleted after ${file.updateDate?.date})"
                            RawCosvFileStatus.FAILED -> " (with errors)"
                            else -> " "
                        }
                    }
                }
            }

            if (leftAvailableFilesCount > 0) {
                li {
                    className = ClassName("list-group-item p-0 d-flex bg-light justify-content-center")
                    buttonBuilder("Load more (left $leftAvailableFilesCount)", isDisabled = isStreamingOperationActive) {
                        fetchMoreFiles()
                    }
                }
            }
        }
    }
}

private fun RawCosvFileDto.isNotSelectable() = status in setOf(RawCosvFileStatus.PROCESSED, RawCosvFileStatus.IN_PROGRESS)

private fun Collection<RawCosvFileDto>.noneWithStatus(status: RawCosvFileStatus) = none { it.status == status }
