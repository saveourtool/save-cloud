@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.domain.Role
import org.cqfn.save.entities.Project
import org.cqfn.save.frontend.externals.fontawesome.faTimesCircle
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.externals.lodash.debounce
import org.cqfn.save.frontend.utils.*
import org.cqfn.save.info.UserInfo
import org.cqfn.save.permission.SetRoleRequest

import csstype.None
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.*
import react.dom.*

import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.hidden
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.js.jso
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cqfn.save.entities.Organization

/**
 * OrganizationSettingsMenu component props
 */
external interface OrganizationSettingsMenuProps : Props {
    /**
     * Current organization settings
     */
    var organization: Organization

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
 * @param updateErrorMessage
 * @return ReactElement
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod"
)
fun organizationSettingsMenu(
    deleteProjectCallback: () -> Unit,
    updateProjectSettings: (Project) -> Unit,
    updateErrorMessage: (Response) -> Unit,
) = fc<OrganizationSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val organizationRef = useRef(props.organization)
    val (draftOrganization, setDraftOrganization) = useState(props.organization)
    useEffect(props.organization) {
        if (organizationRef.current !== props.organization) {
            setDraftProject(props.organization)
            organizationRef.current = props.organization
        }
    }

    val organizationPath = props.organization.name

    val (changeProjectUsers, setChangeProjectUsers) = useState(false)
    val (projectUsers, setProjectUsers) = useState(emptyList<UserInfo>())
    val getProjectUsers = useRequest(dependencies = arrayOf(changeProjectUsers)) {
        val usersFromDb = get(
            url = "$apiUrl/projects/$projectPath/users",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
        )
            .unsafeMap {
                it.decodeFromJsonString<List<UserInfo>>()
            }
        setProjectUsers(usersFromDb)
    }

    val (permissionsChanged, setPermissionsChanged) = useState(mapOf<String, Role>())
    val updatePermissions = useRequest(dependencies = arrayOf(permissionsChanged)) {
        for ((userName, role) in permissionsChanged) {
            val headers = Headers().apply {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
            }
            val response = post(
                "$apiUrl/projects/roles/$projectPath",
                headers,
                Json.encodeToString(SetRoleRequest(userName.split(":")[1], role)),
            )
            if (!response.ok) {
                updateErrorMessage(response)
            }
        }
    }

    val (userToAdd, setUserToAdd) = useState("")
    val (usersNotFromProject, setUsersNotFromProject) = useState(emptyList<UserInfo>())
    val getUsersNotFromProject = debounce(
        useRequest(dependencies = arrayOf(changeProjectUsers, userToAdd)) {
            val headers = Headers().apply {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
            }
            val users = get(
                url = "$apiUrl/users/not-from/$projectPath?prefix=$userToAdd",
                headers = headers,
            )
                .unsafeMap {
                    it.decodeFromJsonString<List<UserInfo>>()
                }
            setUsersNotFromProject(users)
        },
        500,
    )

    val addUserToProject = useRequest {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = post(
            url = "$apiUrl/projects/roles/$projectPath",
            headers = headers,
            body = Json.encodeToString(SetRoleRequest(userToAdd, Role.VIEWER)),
        )
        if (response.ok) {
            setUserToAdd("")
            setChangeProjectUsers { !it }
            getProjectUsers()
            getUsersNotFromProject()
        } else {
            updateErrorMessage(response)
        }
    }

    val (userToDelete, setUserToDelete) = useState(UserInfo(""))
    val deleteUser = useRequest(dependencies = arrayOf(userToDelete)) {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = delete(
            url = "$apiUrl/projects/roles/$projectPath/${userToDelete.name}",
            headers = headers,
            body = Json.encodeToString(userToDelete),
        )
        if (!response.ok) {
            updateErrorMessage(response)
        } else {
            setChangeProjectUsers { !it }
            getProjectUsers()
            getUsersNotFromProject()
        }
    }
    val (isFirstRender, setIsFirstRender) = useState(true)
    if (isFirstRender) {
        getProjectUsers()
        getUsersNotFromProject()
        setIsFirstRender(false)
    }

    val (selfRole, setSelfRole) = useState(props.selfRole)

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

    div("row justify-content-center mb-2") {
        // ===================== LEFT COLUMN =======================================================================
        div("col-4 mb-2 pl-0 pr-0 mr-2 ml-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Users"
            }
            div("card card-body mt-0 pt-0 pr-0 pl-0") {
                div("row mt-0 ml-0 mr-0") {
                    div("input-group") {
                        input(type = InputType.text, classes = "form-control") {
                            attrs.id = "input-users-to-add"
                            attrs.list = "complete-users-to-add"
                            attrs.placeholder = "username"
                            attrs.value = userToAdd
                            attrs.onChangeFunction = {
                                setUserToAdd((it.target as HTMLInputElement).value)
                                getUsersNotFromProject()
                            }
                        }
                        datalist {
                            attrs.id = "complete-users-to-add"
                            attrs["style"] = jso<CSSProperties> {
                                appearance = None.none
                            }
                            for (user in usersNotFromProject) {
                                option {
                                    attrs.value = user.name
                                    attrs.label = user.source ?: ""
                                }
                            }
                        }
                        div("input-group-append") {
                            button(type = ButtonType.button, classes = "btn btn-sm btn-success") {
                                attrs.onClickFunction = {
                                    addUserToProject()
                                }
                                +"Add user"
                            }
                        }
                    }
                }
                for (user in projectUsers) {
                    val userName = user.source + ":" + user.name
                    val userRole = user.projects[projectPath] ?: Role.VIEWER
                    val userIndex = projectUsers.indexOf(user)
                    div("row mt-2 mr-0") {
                        div("col-1") {
                            button(classes = "btn h-auto w-auto") {
                                fontAwesomeIcon(icon = faTimesCircle)
                                attrs.id = "remove-user-$userIndex"
                                attrs.hidden = selfRole.priority <= user.projects[projectPath]!!.priority
                                attrs.onClick = {
                                    val deletedUserIndex = attrs.id.split("-")[2].toInt()
                                    setUserToDelete(projectUsers[deletedUserIndex])
                                    deleteUser()
                                }
                            }
                        }
                        div("col-6 text-left align-self-center") {
                            +userName
                        }
                        div("col-5 text-left align-self-right") {
                            select("custom-select") {
                                attrs.onChangeFunction = { event ->
                                    val target = event.target as HTMLSelectElement
                                    setPermissionsChanged { permissionsChanged ->
                                        permissionsChanged.toMutableMap()
                                            .apply {
                                                put(userName, target.value.toRole())
                                            }
                                            .toMap()
                                    }
                                }
                                attrs.id = "role-$userIndex"
                                for (role in Role.values()) {
                                    if (role != Role.NONE && (role.priority < selfRole.priority ||
                                            user.name == props.currentUserInfo.name && selfRole == role)) {
                                        option {
                                            attrs.value = role.formattedName
                                            attrs.selected = role == userRole
                                            +role.toString()
                                        }
                                    }
                                }

                                attrs.disabled = (permissionsChanged[userName] ?: user.projects[projectPath]!!).priority >= selfRole.priority
                            }
                        }
                    }
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
                                value = draftOrganization.email ?: ""
                                placeholder = "email@example.com"
                                onChange = {
                                    setDraftOrganization(draftOrganization.copy(email = (it.target as HTMLInputElement).value))
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

                hr("") {}
                div("row d-flex justify-content-center") {
                    div("col-3 d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-primary") {
                            attrs.onClickFunction = {
                                updateProjectSettings(draftProject)
                                updatePermissions()
                                getProjectUsers()
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
