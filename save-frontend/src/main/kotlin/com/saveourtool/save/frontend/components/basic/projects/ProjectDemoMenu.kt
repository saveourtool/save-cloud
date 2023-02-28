@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.demo.DemoCreationRequest
import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoInfo
import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.orEmpty
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.fileuploader.simpleFileUploader
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.RoleJs

import csstype.ClassName
import io.ktor.http.*
import react.*
import react.dom.html.AutoComplete
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val demoInfoCard = cardComponent(isBordered = true, hasBg = true, isNoPadding = false)

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "MAGIC_NUMBER"
)
val projectDemoMenu: FC<ProjectDemoMenuProps> = FC { props ->
    val (demoDto, setDemoDto) = useState(
        DemoDto.emptyForProject(props.organizationName, props.projectName)
    )
    val (demoStatus, setDemoStatus) = useState(DemoStatus.NOT_CREATED)
    val (githubProjectCoordinates, setGithubProjectCoordinates) = useState(ProjectCoordinates.empty)
    val (selectedFileDtos, setSelectedFileDtos) = useState(emptyList<FileDto>())
    val sendDemoCreationRequest = useDeferredRequest {
        if (githubProjectCoordinates.consideredBlank()) {
            demoDto.copy(githubProjectCoordinates = null)
        } else {
            demoDto.copy(githubProjectCoordinates = githubProjectCoordinates)
        }
            .let { demoRequest ->
                post(
                    "$apiUrl/demo/${props.organizationName}/${props.projectName}/add",
                    jsonHeaders,
                    Json.encodeToString(
                        DemoCreationRequest(demoRequest, selectedFileDtos)
                    ),
                    ::loadingHandler,
                    ::noopResponseHandler,
                )
                    .let {
                        if (!it.ok) {
                            props.updateErrorMessage(it.statusText, it.unpackMessage())
                        }
                    }
            }
    }

    val getDemoStatus = useDeferredRequest {
        val statusResponse = get(
            "$apiUrl/demo/${props.organizationName}/${props.projectName}/status",
            jsonHeaders,
            ::loadingHandler,
            ::noopResponseHandler,
        )
        if (statusResponse.ok) {
            setDemoStatus(statusResponse.decodeFromJsonString<DemoStatus>())
        } else {
            props.updateErrorMessage(statusResponse.statusText, statusResponse.unpackMessage())
            setDemoStatus(DemoStatus.ERROR)
        }
    }

    useRequest {
        val infoResponse = get(
            "$apiUrl/demo/${props.organizationName}/${props.projectName}",
            jsonHeaders,
            ::loadingHandler,
            ::noopResponseHandler,
        )
        if (infoResponse.ok) {
            val demoInfo: DemoInfo = infoResponse.decodeFromJsonString()
            setDemoStatus(demoInfo.demoStatus)
            setDemoDto(demoInfo.demoDto)
            setGithubProjectCoordinates(demoInfo.demoDto.githubProjectCoordinates.orEmpty())
        } else if (infoResponse.status != 404.toShort()) {
            props.updateErrorMessage(infoResponse.statusText, infoResponse.unpackMessage())
            setDemoStatus(DemoStatus.ERROR)
        }
    }

    useOnce {
        window.alert("This is just a preview, nothing on this view works for now.")
    }

    div {
        className = ClassName("d-flex justify-content-center align-items-center p-2")
        div {
            className = ClassName("col-5")
            demoInfoCard {
                div {
                    className = ClassName("d-flex justify-content-center")
                    renderStatusLabel(demoStatus)
                }
                hr { }
                renderRunSettings(demoDto, setDemoDto, demoStatus != DemoStatus.STOPPED && demoStatus != DemoStatus.NOT_CREATED)
                hr { }
                renderFileUploading(demoStatus, demoDto, setDemoDto, githubProjectCoordinates, setGithubProjectCoordinates, setSelectedFileDtos)
                hr { }
                renderSdkSelector(demoDto, setDemoDto, demoStatus != DemoStatus.STOPPED && demoStatus != DemoStatus.NOT_CREATED)
                hr { }
                renderButtons(demoStatus, props.userProjectRole, sendDemoCreationRequest, getDemoStatus)
            }
        }
    }
}

/**
 * [projectDemoMenu] component [Props]
 */
external interface ProjectDemoMenuProps : Props {
    /**
     * Project name
     */
    var projectName: String

    /**
     * Organization name
     */
    var organizationName: String

    /**
     * User role in project
     */
    var userProjectRole: Role

    /**
     * Callback to show error message
     */
    @Suppress("TYPE_ALIAS")
    var updateErrorMessage: (String, String) -> Unit
}

private fun ChildrenBuilder.renderStatusLabel(demoStatus: DemoStatus) {
    div {
        className = ClassName("col-6 d-flex justify-content-center")
        div {
            val borderStyle = when (demoStatus) {
                DemoStatus.NOT_CREATED -> "border-dark"
                DemoStatus.STARTING -> "border-warning"
                DemoStatus.RUNNING -> "border-success"
                DemoStatus.STOPPED -> "border-secondary"
                DemoStatus.ERROR -> "border-danger"
            }
            className =
                    ClassName("border $borderStyle d-flex align-items-center justify-content-between rounded-pill m-3")
            div {
                className = ClassName("col m-3 flex-wrap")
                label {
                    className = ClassName("m-0")
                    +"Status"
                }
            }
            div {
                className = ClassName("col m-3 flex-wrap")
                label {
                    className = ClassName("m-0")
                    +demoStatus.name
                }
            }
        }
    }
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TOO_MANY_PARAMETERS",
    "TYPE_ALIAS",
    "LongParameterList"
)
private fun ChildrenBuilder.renderFileUploading(
    demoStatus: DemoStatus,
    demoDto: DemoDto,
    setDemoToolRequest: StateSetter<DemoDto>,
    githubProjectCoordinates: ProjectCoordinates,
    setGithubProjectCoordinates: StateSetter<ProjectCoordinates>,
    setSelectedFileDtos: StateSetter<List<FileDto>>,
) {
    div {
        className = ClassName("d-flex justify-content-between align-items-center")
        div {
            className = ClassName("col pl-0")
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "GitHub organization name"
                value = githubProjectCoordinates.organizationName
                disabled = true
                onChange = { event ->
                    setGithubProjectCoordinates {
                        it.copy(organizationName = event.target.value)
                    }
                }
            }
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "GitHub project name"
                value = githubProjectCoordinates.projectName
                disabled = true
                onChange = { event ->
                    setGithubProjectCoordinates {
                        it.copy(projectName = event.target.value)
                    }
                }
            }
            input {
                className = ClassName("form-control col")
                autoComplete = AutoComplete.off
                placeholder = "Release tag"
                value = demoDto.vcsTagName
                disabled = true
                onChange = { event ->
                    setDemoToolRequest { request ->
                        request.copy(vcsTagName = event.target.value)
                    }
                }
            }
        }
        div {
            +" or "
        }
        div {
            className = ClassName("col pr-0")
            div {
                simpleFileUploader {
                    buttonLabel = " Upload files"
                    getUrlForAvailableFilesFetch = {
                        with(demoDto) {
                            "$apiUrl/files/$projectCoordinates/list"
                        }
                    }
                    getUrlForDemoFilesFetch = {
                        with(demoDto) {
                            "$apiUrl/demo/$projectCoordinates/list-file"
                        }
                    }
                    getUrlForFileDeletion = {
                        with(demoDto) {
                            "$apiUrl/demo/$projectCoordinates/delete?fileId=${it.id}"
                        }
                    }
                    getUrlForFileUpload = {
                        with(demoDto) {
                            "$apiUrl/files/$projectCoordinates/upload"
                        }
                    }
                    updateFileDtos = { setSelectedFileDtos(it) }
                    isDisabled = demoStatus == DemoStatus.STARTING || demoStatus == DemoStatus.RUNNING
                }
            }
        }
    }
}

@Suppress("TOO_LONG_FUNCTION")
private fun ChildrenBuilder.renderRunSettings(demoDto: DemoDto, setDemoDto: StateSetter<DemoDto>, disabled: Boolean) {
    div {
        div {
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "Run command"
                value = demoDto.runCommand
                this.disabled = disabled
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(runCommand = event.target.value)
                    }
                }
            }
        }
        div {
            input {
                className = ClassName("form-control col mb-2")
                autoComplete = AutoComplete.off
                placeholder = "Config name"
                value = demoDto.configName
                this.disabled = disabled
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(configName = event.target.value.ifBlank { null })
                    }
                }
            }
        }
        div {
            className = ClassName("d-flex justify-content-between")
            input {
                className = ClassName("form-control col mr-1")
                autoComplete = AutoComplete.off
                placeholder = "Test file name"
                value = demoDto.fileName
                this.disabled = disabled
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(fileName = event.target.value)
                    }
                }
            }
            input {
                className = ClassName("form-control col ml-1")
                autoComplete = AutoComplete.off
                placeholder = "Output file name"
                value = demoDto.outputFileName
                this.disabled = disabled
                onChange = { event ->
                    setDemoDto { request ->
                        request.copy(outputFileName = event.target.value.ifBlank { null })
                    }
                }
            }
        }
    }
}

private fun ChildrenBuilder.renderSdkSelector(demoDto: DemoDto, setDemoDto: StateSetter<DemoDto>, disabled: Boolean) {
    div {
        sdkSelection {
            title = ""
            isDisabled = disabled
            selectedSdk = demoDto.sdk
            onSdkChange = { newSdk ->
                setDemoDto { oldDemoDto ->
                    oldDemoDto.copy(sdk = newSdk)
                }
            }
        }
    }
}

private fun ChildrenBuilder.renderButtons(
    demoStatus: DemoStatus,
    userRole: Role,
    sendDemoCreationRequest: () -> Unit,
    getDemoStatus: () -> Unit,
) {
    div {
        className = ClassName("flex-wrap d-flex justify-content-around")
        when (demoStatus) {
            DemoStatus.NOT_CREATED -> buttonBuilder("Create", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                sendDemoCreationRequest()
            }

            DemoStatus.STARTING -> {
                buttonBuilder("Reload", style = "secondary", isDisabled = userRole.isLowerThan(RoleJs.viewer)) {
                    getDemoStatus()
                }

                buttonBuilder("Stop", style = "danger", isDisabled = userRole.isLowerThan(RoleJs.admin)) {
                    // stop request here
                }
            }

            DemoStatus.RUNNING -> buttonBuilder("Stop", style = "danger", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                // stop request here
            }

            DemoStatus.ERROR, DemoStatus.STOPPED -> {
                buttonBuilder("Run", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    // run request here
                }

                buttonBuilder("Update configuration", style = "info", isDisabled = userRole.isLowerThan(Role.ADMIN)) {
                    // update request here
                }
                buttonBuilder("Delete", style = "danger", isDisabled = userRole.isLowerThan(Role.OWNER)) {
                    // delete request here
                }
            }
        }
    }
}
