@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic.fileuploader

import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.frontend.components.basic.selectFormRequired
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.dragAndDropForm
import com.saveourtool.save.frontend.externals.fontawesome.faDownload
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.FILE_PART_NAME
import com.saveourtool.save.validation.isValidName
import js.core.asList
import org.w3c.fetch.Headers
import react.FC
import react.Props
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.useState
import web.cssom.ClassName
import web.file.File
import web.http.FormData

val cosvFileManagerComponent: FC<Props> = FC { _ ->
    useTooltip()

    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    val organizationSelectForm = selectFormRequired<String>()

    val (cosvAvailableFiles, setCosvAvailableFiles) = useState<List<String>>(emptyList())
    val (filesForUploading, setFilesForUploading) = useState<List<File>>(emptyList())

    val (userOrganizations, setUserOrganizations) = useState(emptyList<OrganizationDto>())
    val (selectOrganizations, setSelectOrganizations) = useState("")

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

    val uploadCosvFiles = useDeferredRequest {
        val uploadedIds: List<String> = post(
            url = "$apiUrl/cosv/batch-upload?organizationName=$selectOrganizations",
            Headers(),
            FormData().apply { filesForUploading.forEach { append(FILE_PART_NAME, it) } },
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler
        ).decodeFromJsonString()
        setCosvAvailableFiles { uploadedIds }
    }

    div {
        organizationSelectForm {
            selectClasses = "custom-select"
            formType = InputTypes.ORGANIZATION_NAME
            validInput = selectOrganizations.isNotEmpty() && selectOrganizations.isValidName()
            classes = "mb-3"
            formName = "Organization"
            getData = { userOrganizations.map { it.name } }
            dataToString = { it }
            selectedValue = selectOrganizations
            disabled = false
            onChangeFun = { value ->
                setSelectOrganizations(value ?: "")
            }
        }

        ul {
            className = ClassName("list-group")
            cosvAvailableFiles.map { file ->
                li {
                    className = ClassName("list-group-item")
                    a {
                        buttonBuilder(faDownload, "", isOutline = true) { }
                        download = file
                        href = "$apiUrl/cosv/get-by-id/$file"
                    }

                    +file
                }
            }

            // ===== UPLOAD FILES BUTTON =====
            li {
                className = ClassName("list-group-item p-0 d-flex bg-light")
                dragAndDropForm {
                    isMultipleFilesSupported = true
                    tooltipMessage = "Only JSON files"
                    onChangeEventHandler = { files ->
                        setFilesForUploading(files!!.asList())
                        uploadCosvFiles()
                    }
                }
            }
        }
    }
}
