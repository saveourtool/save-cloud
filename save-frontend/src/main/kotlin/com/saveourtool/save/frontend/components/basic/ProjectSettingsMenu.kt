@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
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

private val projectPermissionManagerCard = manageUserRoleCardComponent()

private val runSettingGitWindow = runSettingGitWindow()

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
}

/**
 * @param deleteProjectCallback
 * @param updateProjectSettings
 * @param updateErrorMessage
 * @param updateNotificationMessage
 * @param updateGit
 * @return ReactElement
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod"
)
fun projectSettingsMenu(
    deleteProjectCallback: () -> Unit,
    updateProjectSettings: (Project) -> Unit,
    updateGit: (GitDto) -> Unit,
    updateErrorMsg: (Response) -> Unit,
    updateNotificationMessage: (String, String) -> Unit,
) = FC<ProjectSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val projectRef = useRef(props.project)
    val (draftProject, setDraftProject) = useState(props.project)
    val (isGitWindowOpen, setIsGitWindowOpen) = useState(false)

    useEffect(props.project) {
        if (projectRef.current !== props.project) {
            setDraftProject(props.project)
            projectRef.current = props.project
        }
    }

    val projectPath = props.project.let { "${it.organization.name}/${it.name}" }

    val (wasModalShown, setWasModalShown) = useState(false)

    runSettingGitWindow {
        isOpenGitWindow = isGitWindowOpen
        project = props.project
        gitDto = props.gitInitDto
        handlerCancel = { setIsGitWindowOpen(false) }
        onGitUpdate = {
            updateGit(it)
            setIsGitWindowOpen(false)
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
                wasConfirmationModalShown = wasModalShown
                updateErrorMessage = updateErrorMsg
                getUserGroups = { it.projects }
                showGlobalRoleWarning = {
                    updateNotificationMessage(
                        "Super admin message",
                        "Keep in mind that you are super admin, so you are able to manage projects regardless of your organization permissions.",
                    )
                    setWasModalShown(true)
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
                                type = InputType.radio
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
                                type = InputType.radio
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
                                setIsGitWindowOpen(true)
                            }
                            +"Add git credentials"
                        }
                    }
                }

                hr { }
                div {
                    className = ClassName("row d-flex justify-content-center")
                    div {
                        className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-primary")
                            onClick = {
                                updateProjectSettings(draftProject)
                            }
                            +"Save changes"
                        }
                    }
                    div {
                        className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-danger")
                            onClick = {
                                deleteProjectCallback()
                            }
                            +"Delete project"
                        }
                    }
                }
            }
        }
    }
}
