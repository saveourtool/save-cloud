@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.basic

import kotlinx.coroutines.await
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.cqfn.save.agent.TestSuiteExecutionStatisticDto
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.Project

import org.cqfn.save.entities.UserDto
import org.cqfn.save.frontend.components.tables.tableComponent
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap
import org.cqfn.save.frontend.utils.useRequest
import org.cqfn.save.permission.SetRoleRequest
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.*
import react.dom.*

import kotlinx.html.id
import org.cqfn.save.domain.Role
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.table.columns

/**
 * ProjectSettingsMenu component props
 */
external interface ProjectSettingsMenuProps : Props {
    /**
     * List of users connected to the project
     */
    var users: List<UserDto>

    /**
     * Flag to open Menu
     */
    var isOpen: Boolean?

    /**
     * Current project settings
     */
    var project: Project
}

/**
 * @param deleteProjectCallback
 * @param updateProjectSettings
 * @return ReactElement
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod", "MAGIC_NUMBER")
fun projectSettingsMenu(
    openMenuSettingsFlag: (isOpen: Boolean) -> Unit,
    deleteProjectCallback: () -> Unit,
    updateProjectSettings: (Project) -> Unit,
    updatePermissions: (Map<String, Role>) -> Unit,
) = fc<ProjectSettingsMenuProps> { props ->
    var emailFromInput: String? = props.project.email
    var isPublic: Boolean = props.project.public
    var numberOfContainers: String = props.project.numberOfContainers.toString()
    var permissionsChanged: MutableMap<String, Role> = mutableMapOf()

    val (users, setUsers) = useState(props.users)

    useRequest(dependencies = arrayOf(users), isDeferred = false) {
        if (props.isOpen != true) {
            val usersFromDb = get(
                url = "$apiUrl/links/projects/get-by-project?projectName=${props.project.name}&organizationName=${props.project.organization.name}",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
            )
                .unsafeMap {
                    it.decodeFromJsonString<List<UserDto>>()
                }
                .also { println(it) }
            setUsers(usersFromDb)
            openMenuSettingsFlag(true)
        }
    } ()

    div("row justify-content-center mb-2") {
        // ===================== LEFT COLUMN =======================================================================
        div("col-4 mb-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Users"
            }
            child(cardComponent(isBordered = false, hasBg = true) {
                for (user in users) {
                    val userName = user.name ?: "Unknown"
                    var userRole = user.projects[props.project.let { "${it.organization.name}/${it.name}" }] ?: Role.VIEWER
                    div("row mt-2 ml-2 mr-2") {
                        div("col-6 text-left align-self-center") {
                            +(user.name ?: "Unknown")
                        }
                        div("col-6 text-left align-self-center") {
                            select("custom-select") {
//                                attrs.disabled = true
                                attrs.onChangeFunction = {
                                    val target = it.target as HTMLSelectElement
                                    permissionsChanged[userName] = target.value.toRole()
                                    attrs.value = target.value
                                }
                                attrs.id = "numberOfContainers"
                                for (role in Role.values()) {
                                    option {
                                        attrs.value = role.toString()
                                        attrs.selected = role == userRole
                                        +role.toString()
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }
        // ===================== RIGHT COLUMN ======================================================================
        div("col-4 mb-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Main settings"
            }
            child(cardComponent(isBordered = false, hasBg = true) {
                div("row mt-2 ml-2 mr-2") {
                    div("col-5 text-left align-self-center") {
                        +"Project email:"
                    }
                    div("col-7 input-group") {
                        input(type = InputType.email) {
                            attrs["class"] = "form-control"
                            attrs {
                                props.project.email?.let {
                                    defaultValue = it
                                }
                                placeholder = "email@example.com"
                                onChangeFunction = {
                                    emailFromInput = (it.target as HTMLInputElement).value
                                    defaultValue = (it.target as HTMLInputElement).value
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
                                attrs.defaultChecked = isPublic
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
                                attrs.defaultChecked = !isPublic
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
                            isPublic = (it.target as HTMLInputElement).value == "public"
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
                                    val target = it.target as HTMLSelectElement
                                    numberOfContainers = target.value
                                    attrs.value = numberOfContainers
                                }
                                attrs.id = "numberOfContainers"
                                for (i in 1..8) {
                                    option {
                                        attrs.value = i.toString()
                                        attrs.selected = i.toString() == numberOfContainers
                                        +i.toString()
                                    }
                                }
                            }
                        }
                    }
                }

                hr("") {}
                div("row d-flex justify-content-center") {
                    div("col-3 d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-primary") {
                            attrs.onClickFunction = {
                                updateProjectSettings(props.project.copy(
                                    email = emailFromInput,
                                    public = isPublic,
                                    numberOfContainers = numberOfContainers.toInt()
                                ))
                                if (permissionsChanged.isNotEmpty()) {
                                    updatePermissions(permissionsChanged)
                                }
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
            })
        }
    }
}

private fun String.toRole() = when(this) {
    "VIEWER" -> Role.VIEWER
    "SUPER_ADMIN" -> Role.SUPER_ADMIN
    "OWNER" -> Role.OWNER
    "ADMIN" -> Role.ADMIN
    else -> {
        throw IllegalStateException("Unknown role is passed: ${this@toRole}")
    }
}
