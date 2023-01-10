@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoInfo
import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.orEmpty
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.getHighestRole

import csstype.ClassName
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

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "EMPTY_BLOCK_STRUCTURE_ERROR")
val projectDemoMenu: FC<ProjectDemoMenuProps> = FC { props ->
    val (demoDto, setDemoToolRequest) = useState(
        DemoDto.emptyForProject(props.organizationName, props.projectName)
    )
    val (demoStatus, setDemoStatus) = useState(DemoStatus.NOT_CREATED)

    val (githubProjectCoordinates, setGithubProjectCoordinates) = useState(ProjectCoordinates.empty)

    val sendDemoCreationRequest = useDeferredRequest {
        if (githubProjectCoordinates.consideredEmpty()) {
            demoDto
        } else {
            demoDto.copy(githubProjectCoordinates = githubProjectCoordinates)
        }
            .let { demoRequest ->
                post(
                    "$apiUrl/demo/${props.organizationName}/${props.projectName}/add",
                    jsonHeaders,
                    Json.encodeToString(demoRequest),
                    ::loadingHandler,
                    ::noopResponseHandler,
                )
                    .let {
                        if (it.ok) {
                            setDemoStatus(DemoStatus.STARTING)
                        } else {
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
            setDemoStatus(DemoStatus.NOT_CREATED)
        }
    }

    props.projectRole.isHigherOrEqualThan(Role.ADMIN).let {
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
                setDemoToolRequest(demoInfo.demoDto)
                setGithubProjectCoordinates(demoInfo.demoDto.githubProjectCoordinates.orEmpty())
            } else {
                props.updateErrorMessage(infoResponse.statusText, infoResponse.unpackMessage())
                setDemoStatus(DemoStatus.NOT_CREATED)
            }
        }
    }

    useOnce {
        window.alert("This is just a preview, nothing on this view works for now.")
    }

    div {
        className = ClassName("d-flex justify-content-center p-2")
        div {
            demoInfoCard {
                div {
                    val borderStyle = when (demoStatus) {
                        DemoStatus.NOT_CREATED -> "border-dark"
                        DemoStatus.STARTING -> "border-warning"
                        DemoStatus.RUNNING -> "border-success"
                        DemoStatus.STOPPED -> "border-danger"
                    }
                    className = ClassName("border $borderStyle rounded-pill row m-3")
                    label {
                        className = ClassName("col m-3")
                        +"Status"
                    }
                    label {
                        className = ClassName("col m-3")
                        +demoStatus.name
                    }
                }
                hr { }
                input {
                    className = ClassName("form-control col mb-2")
                    autoComplete = AutoComplete.off
                    placeholder = "GitHub organization name"
                    value = githubProjectCoordinates.organizationName
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
                    onChange = { event ->
                        setDemoToolRequest { request ->
                            request.copy(vcsTagName = event.target.value)
                        }
                    }
                }
                hr { }
                input {
                    className = ClassName("form-control col mb-2")
                    autoComplete = AutoComplete.off
                    placeholder = "Run command"
                    value = demoDto.runCommand
                    onChange = { event ->
                        setDemoToolRequest { request ->
                            request.copy(runCommand = event.target.value)
                        }
                    }
                }
                input {
                    className = ClassName("form-control col")
                    autoComplete = AutoComplete.off
                    placeholder = "Test file name"
                    value = demoDto.fileName
                    onChange = { event ->
                        setDemoToolRequest { request ->
                            request.copy(fileName = event.target.value)
                        }
                    }
                }
                hr { }
                div {
                    className = ClassName("flex-wrap d-flex justify-content-around")
                    when (demoStatus) {
                        DemoStatus.NOT_CREATED -> if (getHighestRole(props.organizationRole, props.projectRole).isHigherOrEqualThan(Role.OWNER)) {
                            buttonBuilder("Create") {
                                sendDemoCreationRequest()
                            }
                        }
                        DemoStatus.STARTING -> if (props.projectRole.isHigherOrEqualThan(Role.VIEWER) || props.organizationRole.isHigherOrEqualThan(Role.OWNER)) {
                            buttonBuilder("Reload") {
                                getDemoStatus()
                            }
                        }
                        DemoStatus.RUNNING -> {
                            buttonBuilder("Restart") {
                                // restart request here
                            }

                            buttonBuilder("Stop") {
                                setDemoStatus(DemoStatus.STOPPED)
                                // stop request here
                            }
                        }
                        DemoStatus.STOPPED -> buttonBuilder("Run") {
                            setDemoStatus(DemoStatus.RUNNING)
                            // run request here
                        }
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
     * The user role in the project
     */
    var projectRole: Role

    /**
     * The user role in the organization
     */
    var organizationRole: Role

    /**
     * Callback to show error message
     */
    @Suppress("TYPE_ALIAS")
    var updateErrorMessage: (String, String) -> Unit
}
