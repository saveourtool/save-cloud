@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.demo.DemoCreationRequest
import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.demo.RunCommandPair
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.orEmpty
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.demo.*
import com.saveourtool.save.frontend.components.basic.demo.management.*
import com.saveourtool.save.frontend.components.basic.demo.management.renderButtons
import com.saveourtool.save.frontend.components.basic.demo.management.renderFileUploading
import com.saveourtool.save.frontend.components.basic.demo.management.renderSdkSelector
import com.saveourtool.save.frontend.components.basic.demo.management.renderStatusLabel
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import io.ktor.http.*
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val demoInfoCard = cardComponent(isBordered = true, hasBg = true, isNoPadding = false)

/**
 * Project demo menu
 */
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

    val getDemoDto = useDeferredRequest {
        val dtoResponse = get(
            "$demoApiUrl/manager/${props.organizationName}/${props.projectName}",
            jsonHeaders,
            ::loadingHandler,
            ::noopResponseHandler,
        )
        if (dtoResponse.ok) {
            val dto: DemoDto = dtoResponse.decodeFromJsonString()
            setDemoDto(dto)
            setGithubProjectCoordinates(dto.githubProjectCoordinates.orEmpty())
        } else if (dtoResponse.status != 404.toShort()) {
            props.updateErrorMessage(dtoResponse.statusText, dtoResponse.unpackMessage())
            setDemoStatus(DemoStatus.ERROR)
        } else {
            demoDto.copy(githubProjectCoordinates = githubProjectCoordinates)
        }
    }

    val deleteDemo = useDeferredRequest {
        post(
            "$apiUrl/demo/${props.organizationName}/${props.projectName}/delete",
            jsonHeaders,
            Unit,
            loadingHandler = ::loadingHandler,
        ).let {
            if (it.ok) {
                window.alert(it.body as String)
                setDemoStatus(DemoStatus.NOT_CREATED)
                setDemoDto(DemoDto.emptyForProject(props.organizationName, props.projectName))
                setGithubProjectCoordinates(ProjectCoordinates.empty)
            }
        }
    }

    val getDemoStatus = useDeferredRequest {
        val statusResponse = get(
            "$demoApiUrl/manager/${props.organizationName}/${props.projectName}/status",
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

    val createDemo = useDeferredRequest {
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
                        if (it.ok) {
                            setDemoStatus(DemoStatus.STOPPED)
                            window.alert(it.body as String)
                        } else {
                            props.updateErrorMessage(it.statusText, it.unpackMessage())
                        }
                    }
            }
    }

    val startDemo = useDeferredRequest {
        post(
            "$apiUrl/demo/${props.organizationName}/${props.projectName}/start",
            jsonHeaders,
            Unit,
            ::loadingHandler,
        ).let {
            if (it.ok) {
                setDemoStatus(DemoStatus.STARTING)
                window.alert(it.body as String)
            }
        }
    }

    val stopDemo = useDeferredRequest {
        post(
            "$apiUrl/demo/${props.organizationName}/${props.projectName}/stop",
            jsonHeaders,
            Unit,
            ::loadingHandler,
        ).let {
            if (it.ok) {
                setDemoStatus(DemoStatus.STOPPING)
                window.alert(it.body as String)
            }
        }
    }

    useOnce {
        getDemoDto()
        getDemoStatus()
    }

    val (selectedModeCommand, setSelectedModeCommand) = useState<RunCommandPair?>(null)
    val isDisabled = demoStatus != DemoStatus.STOPPED && demoStatus != DemoStatus.NOT_CREATED
    demoModeModal(selectedModeCommand, setSelectedModeCommand, isDisabled, setDemoDto)

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
                renderDemoSettings(demoDto, setDemoDto)
                hr { }
                renderRunCommand(demoDto, setDemoDto, isDisabled, setSelectedModeCommand)
                hr { }
                renderFileUploading(demoStatus, demoDto, setDemoDto, githubProjectCoordinates, setGithubProjectCoordinates, setSelectedFileDtos)
                hr { }
                renderSdkSelector(demoDto, setDemoDto, isDisabled)
                hr { }
                renderButtons(
                    demoStatus,
                    props.userProjectRole,
                    createDemo,
                    getDemoStatus,
                    startDemo,
                    stopDemo
                ) {
                    if (window.confirm("Delete demo of ${props.organizationName}/${props.projectName}?")) {
                        deleteDemo()
                        setDemoStatus(DemoStatus.STOPPING)
                    }
                }
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
