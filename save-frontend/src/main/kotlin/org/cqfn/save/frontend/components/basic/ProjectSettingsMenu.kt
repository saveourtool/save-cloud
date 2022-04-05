@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.unsafeMap
import org.cqfn.save.frontend.utils.useRequest
import org.cqfn.save.info.UserInfo

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.fetch.Headers
import react.*
import react.dom.*

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction

/**
 * ProjectSettingsMenu component props
 */
external interface ProjectSettingsMenuProps : Props {
    /**
     * List of users connected to the project
     */
    var users: List<UserInfo>

    /**

     * Flag to open Menu
     */
    var isOpen: Boolean?

    /**
     * Current project settings
     */
    var project: Project

    /**
     * Role of user that opened this window
     */
    var selfRole: Role

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo
}

private fun String.toRole() = when (this) {
    Role.VIEWER.toString(), Role.VIEWER.formattedName -> Role.VIEWER
    Role.SUPER_ADMIN.toString(), Role.SUPER_ADMIN.formattedName -> Role.SUPER_ADMIN
    Role.OWNER.toString(), Role.OWNER.formattedName -> Role.OWNER
    Role.ADMIN.toString(), Role.ADMIN.formattedName -> Role.ADMIN
    Role.NONE.toString(), Role.NONE.formattedName -> Role.NONE
    else -> throw IllegalStateException("Unknown role is passed: $this")
}

/**
 * @param deleteProjectCallback
 * @param updateProjectSettings
 * @param openMenuSettingsFlag
 * @param updatePermissions
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
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    var permissionsChanged: MutableMap<String, Role> = mutableMapOf()

    val (users, setUsers) = useState(props.users)

    getUsers(props, setUsers, openMenuSettingsFlag)

    val projectPath = props.project.let { "${it.organization.name}/${it.name}" }

    val (selfRole, setSelfRole) = useState(props.selfRole)

    getSelfRole(projectPath, setSelfRole)

    div("row justify-content-center mb-2") {
        // ===================== LEFT COLUMN =======================================================================
        div("col-4 mb-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Users"
            }
            child(cardComponent(isBordered = false, hasBg = true) {
                for (user in users) {
                    val userName = user.source + ":" + (user.name)
                    val userRole = user.projects[projectPath] ?: Role.VIEWER
                    div("row mt-2 ml-2 mr-2") {
                        div("col-6 text-left align-self-center") {
                            +userName
                        }
                        div("col-6 text-left align-self-center") {
                            select("custom-select") {
                                attrs.onChangeFunction = {
                                    val target = it.target as HTMLSelectElement
                                    permissionsChanged[userName] = target.value.toRole()
                                    attrs.value = target.value
                                }
                                attrs.id = "role${users.indexOf(user)}"
                                for (role in Role.values()) {
                                    if (role != Role.NONE) {
                                        option {
                                            attrs.value = role.formattedName
                                            attrs.selected = role == userRole
                                            +role.toString()
                                            attrs.disabled = role.priority >= (selfRole.priority)
                                        }
                                    }
                                }
                                attrs.disabled = (permissionsChanged[userName] ?: user.projects[projectPath]!!).priority >= selfRole.priority
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
                    div("col-7 input-group pl-0") {
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
                                if (permissionsChanged.isNotEmpty()) {
                                    updatePermissions(permissionsChanged)
                                    getUsers(props, setUsers, openMenuSettingsFlag)
                                    permissionsChanged = mutableMapOf()
                                }
                                updateProjectSettings(props.project.copy(
                                    email = emailFromInput,
                                    public = isPublic,
                                    numberOfContainers = numberOfContainers.toInt()
                                ))
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
@Suppress("TYPE_ALIAS")
private fun getUsers(
    props: ProjectSettingsMenuProps,
    setUsers: StateSetter<List<UserInfo>>,
    openMenuSettingsFlag: (isOpen: Boolean) -> Unit,
) {
    useRequest(isDeferred = false) {
        if (props.isOpen != true) {
            val usersFromDb = get(
                url = "$apiUrl/projects/${props.project.organization.name}/${props.project.name}/users",
                headers = Headers().also {
                    it.set("Accept", "application/json")
                },
            )
                .unsafeMap {
                    it.decodeFromJsonString<List<UserInfo>>()
                }
            setUsers(usersFromDb)
            openMenuSettingsFlag(true)
        }
    }()
}

private fun getSelfRole(projectPath: String, setSelfRole: StateSetter<Role>) {
    useRequest(isDeferred = false) {
        val role = get(
            "$apiUrl/projects/roles/$projectPath",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
        )
            .unsafeMap {
                it.decodeFromJsonString<String>()
            }
            .toRole()
        setSelfRole(role)
    }()
}
