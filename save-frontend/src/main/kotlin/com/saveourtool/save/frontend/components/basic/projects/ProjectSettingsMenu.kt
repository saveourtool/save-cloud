@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.components.basic.gitWindow
import com.saveourtool.save.frontend.components.basic.manageUserRoleCardComponent
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo

import csstype.ClassName
import org.w3c.dom.HTMLInputElement
import org.w3c.fetch.Response
import react.*
import react.dom.*
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select

import kotlinx.html.id

private val projectPermissionManagerCard = manageUserRoleCardComponent()

/**
 * SETTINGS tab in ProjectView
 */
val projectSettingsMenu = projectSettingsMenu()

/**
 * ProjectSettingsMenu component props
 */
external interface ProjectSettingsMenuProps : Props {
    /**
     * Current project settings
     */
    var project: Project

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo

    /**
     * Git data for project
     */
    var gitInitDto: GitDto?

    /**
     * Role of a current user
     */
    var selfRole: Role

    /**
     * Callback to delete project
     */
    var deleteProjectCallback: () -> Unit

    /**
     * Callback to update project settings
     */
    var updateProjectSettings: (Project) -> Unit

    /**
     * Callback to update git
     */
    var updateGit: (GitDto) -> Unit

    /**
     * Callback to show error message
     */
    var updateErrorMessage: (Response) -> Unit

    /**
     * Callback to show notification message
     */
    var updateNotificationMessage: (String, String) -> Unit
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR"
)
private fun projectSettingsMenu() = FC<ProjectSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val projectRef = useRef(props.project)
    val (draftProject, setDraftProject) = useState(props.project)
    val (isOpenGitWindow, setOpenGitWindow) = useState(false)

    useEffect(props.project) {
        if (projectRef.current !== props.project) {
            setDraftProject(props.project)
            projectRef.current = props.project
        }
    }

    val projectPath = props.project.let { "${it.organization.name}/${it.name}" }

    val (wasConfirmationModalShown, setWasConfirmationModalShown) = useState(false)

    gitWindow {
        this.isOpenGitWindow = isOpenGitWindow
        project = props.project
        gitDto = props.gitInitDto
        handlerCancel = { setOpenGitWindow(false) }
        onGitUpdate = {
            props.updateGit(it)
            setOpenGitWindow(false)
        }
    }

    div {
        className = ClassName("row justify-content-center mb-2")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Users"
            }
            projectPermissionManagerCard {
                selfUserInfo = props.currentUserInfo
                groupPath = projectPath
                groupType = "project"
                this.wasConfirmationModalShown = wasConfirmationModalShown
                updateErrorMessage = props.updateErrorMessage
                getUserGroups = { it.projects }
                showGlobalRoleWarning = {
                    props.updateNotificationMessage(
                        "Super admin message",
                        "Keep in mind that you are super admin, so you are able to manage projects regardless of your organization permissions.",
                    )
                    setWasConfirmationModalShown(true)
                }
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Main settings"
            }
            div {
                className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0")
                div {
                    className = ClassName("row mt-2 ml-2 mr-2")
                    div {
                        className = ClassName("col-5 text-left align-self-center")
                        +"Project email:"
                    }
                    div {
                        className = ClassName("col-7 input-group pl-0")
                        input {
                            type = InputType.email
                            className = ClassName("form-control")
                            value = draftProject.email ?: ""
                            placeholder = "email@example.com"
                            onChange = {
                                setDraftProject(draftProject.copy(email = it.target.value))
                            }
                        }
                    }
                }
                div {
                    className = ClassName("row mt-2 ml-2 mr-2")
                    div {
                        className = ClassName("col-5 text-left align-self-center")
                        +"Project visibility:"
                    }
                    form {
                        className = ClassName("col-7 form-group row d-flex justify-content-around")
                        div {
                            className = ClassName("form-check-inline")
                            input {
                                className = ClassName("form-check-input")
                                defaultChecked = draftProject.public
                                name = "projectVisibility"
                                type = react.dom.html.InputType.radio
                                id = "isProjectPublicSwitch"
                                value = "public"
                            }
                            label {
                                className = ClassName("form-check-label")
                                htmlFor = "isProjectPublicSwitch"
                                +"Public"
                            }
                        }
                        div {
                            className = ClassName("form-check-inline")
                            input {
                                className = ClassName("form-check-input")
                                defaultChecked = !draftProject.public
                                name = "projectVisibility"
                                type = react.dom.html.InputType.radio
                                id = "isProjectPrivateSwitch"
                                value = "private"
                            }
                            label {
                                className = ClassName("form-check-label")
                                htmlFor = "isProjectPrivateSwitch"
                                +"Private"
                            }
                        }
                        onChange = {
                            setDraftProject(draftProject.copy(public = (it.target as HTMLInputElement).value == "public"))
                        }
                    }
                }
                div {
                    className = ClassName("row d-flex align-items-center mt-2 mr-2 ml-2")
                    div {
                        className = ClassName("col-5 text-left")
                        +"Number of containers:"
                    }
                    div {
                        className = ClassName("col-7 row")
                        div {
                            className = ClassName("form-switch")
                            select {
                                className = ClassName("custom-select")
                                // fixme: later we will need to change amount of containers
                                disabled = true
                                onChange = {
                                    setDraftProject(draftProject.copy(numberOfContainers = it.target.value.toInt()))
                                }
                                id = "numberOfContainers"
                                for (i in 1..8) {
                                    option {
                                        value = i.toString()
                                        selected = i == draftProject.numberOfContainers
                                        +i.toString()
                                    }
                                }
                            }
                        }
                    }
                }
                div {
                    className = ClassName("row d-flex align-items-center mt-2 mr-2 ml-2")
                    div {
                        className = ClassName("col-5 text-left")
                        +"User settings:"
                    }
                    div {
                        className = ClassName("col-7 row")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-primary")
                            onClick = {
                                setOpenGitWindow(true)
                            }
                            +"Add git credentials"
                        }
                    }
                }

                hr {}
                div {
                    className = ClassName("row d-flex justify-content-center")
                    div {
                        className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-primary")
                            onClick = {
                                props.updateProjectSettings(draftProject)
                            }
                            +"Save changes"
                        }
                    }
                    div {
                        className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-danger")
                            disabled = !props.selfRole.hasDeletePermission()
                            onClick = {
                                props.deleteProjectCallback()
                            }
                            +"Delete project"
                        }
                    }
                }
            }
        }
    }
}
