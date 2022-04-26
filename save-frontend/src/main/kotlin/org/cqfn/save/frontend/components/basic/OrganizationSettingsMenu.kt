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
    deleteOrganizationCallback: () -> Unit,
    updateOrganizationSettings: (Organization) -> Unit,
    updateErrorMessage: (Response) -> Unit,
) = fc<OrganizationSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val organizationRef = useRef(props.organization)
    val (draftOrganization, setDraftOrganization) = useState(props.organization)
    useEffect(props.organization) {
        if (organizationRef.current !== props.organization) {
            setDraftOrganization(props.organization)
            organizationRef.current = props.organization
        }
    }

    val organizationPath = props.organization.name

    val (changeOrganizationUsers, setChangeOrganizationUsers) = useState(false)
    val (organizationUsers, setOrganizationUsers) = useState(emptyList<UserInfo>())
    val getOrganizationUsers = useRequest(dependencies = arrayOf(changeOrganizationUsers)) {
        val usersFromDb = get(
            url = "$apiUrl/organization/$organizationPath/users",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
        )
            .unsafeMap {
                it.decodeFromJsonString<List<UserInfo>>()
            }
        setOrganizationUsers(usersFromDb)
    }

    val (permissionsChanged, setPermissionsChanged) = useState(mapOf<String, Role>())
    val updatePermissions = useRequest(dependencies = arrayOf(permissionsChanged)) {
        for ((userName, role) in permissionsChanged) {
            val headers = Headers().apply {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
            }
            val response = post(
                "$apiUrl/projects/roles/$organizationPath",
                headers,
                Json.encodeToString(SetRoleRequest(userName.split(":")[1], role)),
            )
            if (!response.ok) {
                updateErrorMessage(response)
            }
        }
    }

    val (userToAdd, setUserToAdd) = useState(UserInfo(""))
    val (usersNotFromOrganization, setUsersNotFromOrganization) = useState(emptyList<UserInfo>())
    val getUsersNotFromOrganization = debounce(
        useRequest(dependencies = arrayOf(changeOrganizationUsers, userToAdd)) {
            val headers = Headers().apply {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
            }
            val users = get(
                url = "$apiUrl/users/not-from/$organizationPath?prefix=${userToAdd.name}",
                headers = headers,
            )
                .unsafeMap {
                    it.decodeFromJsonString<List<UserInfo>>()
                }
            setUsersNotFromOrganization(users)
        },
        500,
    )

    val addUserToOrganization = useRequest {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = post(
            url = "$apiUrl/organizations/roles/$organizationPath",
            headers = headers,
            body = Json.encodeToString(SetRoleRequest(userToAdd.name, Role.VIEWER)),
        )
        if (response.ok) {
            setUserToAdd(UserInfo(""))
            setChangeOrganizationUsers { !it }
            getOrganizationUsers()
            getUsersNotFromOrganization()
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
            url = "$apiUrl/organizations/roles/$organizationPath/${userToDelete.name}",
            headers = headers,
            body = Json.encodeToString(userToDelete),
        )
        if (!response.ok) {
            updateErrorMessage(response)
        } else {
            setChangeOrganizationUsers { !it }
            getOrganizationUsers()
            getUsersNotFromOrganization()
        }
    }
    val (isFirstRender, setIsFirstRender) = useState(true)
    if (isFirstRender) {
        getOrganizationUsers()
        setIsFirstRender(false)
    }

    val (selfRole, setSelfRole) = useState(props.selfRole)

    useRequest(isDeferred = false) {
        val role = get(
            "$apiUrl/organizations/roles/$organizationPath",
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

    val organizationPermissionManagerCard = manageUserRoleCardComponent(
        {
            setUserToAdd(it)
            addUserToOrganization()
        },
        {
            setUserToDelete(it)
            deleteUser()
        },
        {
            it.organizations
        },
    )

    div("row justify-content-center mb-2") {
        // ===================== LEFT COLUMN =======================================================================
        div("col-4 mb-2 pl-0 pr-0 mr-2 ml-2") {
            div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                +"Users"
            }
            child(organizationPermissionManagerCard) {
                attrs.selfUserInfo = props.currentUserInfo
                attrs.usersFromGroup = organizationUsers
                attrs.usersNotFromGroup = usersNotFromOrganization
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
                }

                hr("") {}
                div("row d-flex justify-content-center") {
                    div("col-3 d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-primary") {
                            attrs.onClickFunction = {
                                updateOrganizationSettings(draftOrganization)
                                updatePermissions()
                                getOrganizationUsers()
                            }
                            +"Save changes"
                        }
                    }
                    div("col-3 d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-sm btn-danger") {
                            attrs.onClickFunction = {
                                deleteOrganizationCallback()
                            }
                            +"Delete project"
                        }
                    }
                }
            }
        }
    }
}
