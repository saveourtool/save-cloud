@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.fetch.Response
import react.*
import react.dom.*

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction

private val projectPermissionManagerCard = manageUserRoleCardComponent()

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
    updateErrorMessage: (Response) -> Unit,
    updateNotificationMessage: (String, String) -> Unit,
) = fc<ProjectSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val projectRef = useRef(props.project)
    val (draftProject, setDraftProject) = useState(props.project)
    useEffect(props.project) {
        if (projectRef.current !== props.project) {
            setDraftProject(props.project)
            projectRef.current = props.project
        }
    }

    val projectPath = props.project.let { "${it.organization.name}/${it.name}" }

    val (wasConfirmationModalShown, setWasConfirmationModalShown) = useState(false)
    val (isOpenGitWindow, setOpenGitWindow) = useState(false)
    val (gitDto, setGitDto) = useState(props.gitInitDto)

    child(
        runSettingGitWindow(
            isOpenGitWindow,
            props.project,
            gitDto,
            handlerCancel = { setOpenGitWindow(false) },
            handler = {
                setGitDto(it)
                setOpenGitWindow(false)
            }
        )
    )

    div("row justify-content-center mb-2") {
        // ===================== LEFT COLUMN =======================================================================
        div("col-4 mb-2 pl-0 pr-0 mr-2 ml-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Users"
            }
            child(projectPermissionManagerCard) {
                attrs.selfUserInfo = props.currentUserInfo
                attrs.groupPath = projectPath
                attrs.groupType = "project"
                attrs.wasConfirmationModalShown = wasConfirmationModalShown
                attrs.updateErrorMessage = updateErrorMessage
                attrs.getUserGroups = { it.projects }
                attrs.showGlobalRoleWarning = {
                    updateNotificationMessage(
                        "Super admin message",
                        "Keep in mind that you are super admin, so you are able to manage projects regardless of your organization permissions.",
                    )
                    setWasConfirmationModalShown(true)
                }
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div("col-4 mb-2 pl-0 pr-0 mr-2 ml-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Main settings"
            }
            div("card card-body mt-0 pt-0 pr-0 pl-0") {
                div("row mt-2 ml-2 mr-2") {
                    div("col-5 text-left align-self-center") {
                        +"Project email:"
                    }
                    div("col-7 input-group pl-0") {
                        input(type = InputType.email) {
                            attrs["class"] = "form-control"
                            attrs {
                                value = draftProject.email ?: ""
                                placeholder = "email@example.com"
                                onChange = {
                                    setDraftProject(draftProject.copy(email = (it.target as HTMLInputElement).value))
                                }
                            }
                        }
                    }
                }
                div("row mt-2 ml-2 mr-2") {
                    div("col-5 text-left align-self-center") {
                        +"Project visibility:"
                    }
                    form("col-7 form-group row d-flex justify-content-around") {
                        div("form-check-inline") {
                            input(classes = "form-check-input") {
                                attrs.defaultChecked = draftProject.public
                                attrs["name"] = "projectVisibility"
                                attrs["type"] = "radio"
                                attrs["id"] = "isProjectPublicSwitch"
                                attrs["value"] = "public"
                            }
                            label("form-check-label") {
                                attrs["htmlFor"] = "isProjectPublicSwitch"
                                +"Public"
                            }
                        }
                        div("form-check-inline") {
                            input(classes = "form-check-input") {
                                attrs.defaultChecked = !draftProject.public
                                attrs["name"] = "projectVisibility"
                                attrs["type"] = "radio"
                                attrs["id"] = "isProjectPrivateSwitch"
                                attrs["value"] = "private"
                            }
                            label("form-check-label") {
                                attrs["htmlFor"] = "isProjectPrivateSwitch"
                                +"Private"
                            }
                        }
                        attrs.onChangeFunction = {
                            setDraftProject(draftProject.copy(public = (it.target as HTMLInputElement).value == "public"))
                        }
                    }
                }
                div("row d-flex align-items-center mt-2 mr-2 ml-2") {
                    div("col-5 text-left") {
                        +"Number of containers:"
                    }
                    div("col-7 row") {
                        div("form-switch") {
                            select("custom-select") {
                                // fixme: later we will need to change amount of containers
                                attrs.disabled = true
                                attrs.onChangeFunction = {
                                    setDraftProject(draftProject.copy(numberOfContainers = (it.target as HTMLSelectElement).value.toInt()))
                                }
                                attrs.id = "numberOfContainers"
                                for (i in 1..8) {
                                    option {
                                        attrs.value = i.toString()
                                        attrs.selected = i == draftProject.numberOfContainers
                                        +i.toString()
                                    }
                                }
                            }
                        }
                    }
                }
                div("row d-flex align-items-center mt-2 mr-2 ml-2") {
                    div("col-5 text-left") {
                        +"User settings:"
                    }
                    div("col-7 row") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-primary") {
                            attrs.onClickFunction = {
                                setOpenGitWindow(true)
                            }
                            +"Add git credentials"
                        }
                    }
                }

                hr("") {}
                div("row d-flex justify-content-center") {
                    div("col-3 d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-primary") {
                            attrs.onClickFunction = {
                                updateProjectSettings(draftProject)
                            }
                            +"Save changes"
                        }
                    }
                    div("col-3 d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-danger") {
                            attrs.onClickFunction = {
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
