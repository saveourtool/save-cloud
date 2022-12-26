@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.demo.NewDemoToolRequest
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.utils.*

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
    val (demoToolRequest, setDemoToolRequest) = useState(NewDemoToolRequest.empty)
    val (demoStatus, setDemoStatus) = useState(DemoStatus.NOT_CREATED)

    val sendDemoRequest = useDeferredRequest {
        post(
            "$apiUrl/demo/add",
            jsonHeaders,
            Json.encodeToString(demoToolRequest),
            ::loadingHandler,
        )
    }

    val getDemoStatus = useDeferredRequest {
        val statusResponse = get(
            "$apiUrl/demo/${demoToolRequest.organizationName}/${demoToolRequest.projectName}",
            jsonHeaders,
            ::loadingHandler,
            ::noopResponseHandler,
        )
        if (statusResponse.ok) {
            setDemoStatus(statusResponse.decodeFromJsonString<DemoStatus>())
        } else {
            setDemoStatus(DemoStatus.NOT_CREATED)
        }
    }

    useOnce {
        window.alert("This is just a preview, nothing on this view works for now.")
        getDemoStatus()
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
                    value = demoToolRequest.organizationName
                    onChange = { event ->
                        setDemoToolRequest { request ->
                            request.copy(organizationName = event.target.value)
                        }
                    }
                }
                input {
                    className = ClassName("form-control col mb-2")
                    autoComplete = AutoComplete.off
                    placeholder = "GitHub project name"
                    value = demoToolRequest.projectName
                    onChange = { event ->
                        setDemoToolRequest { request ->
                            request.copy(projectName = event.target.value)
                        }
                    }
                }
                input {
                    className = ClassName("form-control col")
                    autoComplete = AutoComplete.off
                    placeholder = "Release tag"
                    value = demoToolRequest.vcsTagName
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
                    value = demoToolRequest.runCommand
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
                    value = demoToolRequest.fileName
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
                        DemoStatus.NOT_CREATED -> buttonBuilder("Create") {
                            sendDemoRequest()
                            setDemoStatus(DemoStatus.STARTING)
                        }
                        DemoStatus.STARTING -> buttonBuilder("Reload") {
                            getDemoStatus()
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
}
