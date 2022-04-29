/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.domain.Role
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
import react.CSSProperties
import react.PropsWithChildren
import react.dom.*
import react.dom.onClick
import react.fc
import react.useState

import kotlinx.html.*
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.js.jso
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [RProps] for card component
 */
external interface ManageUserRoleCardProps : PropsWithChildren {
    /**
     * Information about user who is seeing the view
     */
    var selfUserInfo: UserInfo

    /**
     * Full name of a group
     */
    var groupPath: String

    /**
     * Kind of a group that will be shown ("project" or "organization" for now)
     */
    var groupType: String
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
 * A functional `RComponent` for a card that shows users from the group and their permissions.
 *
 * @param updateErrorMessage
 * @param getUserGroups
 * @return a functional component representing a role managing card
 */
@Suppress(
    "LongMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "TOO_LONG_FUNCTION",
    "MAGIC_NUMBER",
    "ComplexMethod",
)
fun manageUserRoleCardComponent(
    updateErrorMessage: (Response) -> Unit,
    getUserGroups: (UserInfo) -> Map<String, Role>,
) = fc<ManageUserRoleCardProps> { props ->

    val (changeUsersFromGroup, setChangeUsersFromGroup) = useState(true)
    val (usersFromGroup, setUsersFromGroup) = useState(emptyList<UserInfo>())
    val getUsersFromGroup = useRequest(dependencies = arrayOf(changeUsersFromGroup)) {
        val usersFromDb = get(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users",
            headers = Headers().also {
                it.set("Accept", "application/json")
            },
        )
            .unsafeMap {
                it.decodeFromJsonString<List<UserInfo>>()
            }
        setUsersFromGroup(usersFromDb)
    }

    val (roleChange, setRoleChange) = useState(SetRoleRequest("", Role.NONE))
    val updatePermissions = useRequest(dependencies = arrayOf(roleChange)) {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = post(
            "$apiUrl/${props.groupType}s/roles/${props.groupPath}",
            headers,
            Json.encodeToString(roleChange),
        )
        if (!response.ok) {
            updateErrorMessage(response)
        } else {
            getUsersFromGroup()
        }
    }

    val (userToAdd, setUserToAdd) = useState(UserInfo(""))
    val (usersNotFromGroup, setUsersNotFromGroup) = useState(emptyList<UserInfo>())
    val getUsersNotFromGroup = debounce(
        useRequest(dependencies = arrayOf(changeUsersFromGroup, userToAdd)) {
            val headers = Headers().apply {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
            }
            val users = get(
                url = "$apiUrl/${props.groupType}s/${props.groupPath}/users/not-from?prefix=${userToAdd.name}",
                headers = headers,
            )
                .unsafeMap {
                    it.decodeFromJsonString<List<UserInfo>>()
                }
            setUsersNotFromGroup(users)
        },
        500,
    )
    val addUserToGroup = useRequest {
        val headers = Headers().apply {
            set("Accept", "application/json")
            set("Content-Type", "application/json")
        }
        val response = post(
            url = "$apiUrl/${props.groupType}s/roles/${props.groupPath}",
            headers = headers,
            body = Json.encodeToString(SetRoleRequest(userToAdd.name, Role.VIEWER)),
        )
        if (response.ok) {
            setUserToAdd(UserInfo(""))
            setChangeUsersFromGroup { !it }
            getUsersFromGroup()
            setUsersNotFromGroup(emptyList())
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
            url = "$apiUrl/${props.groupType}s/roles/${props.groupPath}/${userToDelete.name}",
            headers = headers,
            body = Json.encodeToString(userToDelete),
        )
        if (!response.ok) {
            updateErrorMessage(response)
        } else {
            setChangeUsersFromGroup { !it }
            getUsersFromGroup()
            setUsersNotFromGroup(emptyList())
        }
    }

    val (selfRole, setSelfRole) = useState(Role.NONE)
    useRequest(isDeferred = false) {
        val role = get(
            "$apiUrl/${props.groupType}s/roles/${props.groupPath}",
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

    val (isFirstRender, setIsFirstRender) = useState(true)
    if (isFirstRender) {
        getUsersFromGroup()
        setIsFirstRender(false)
    }

    div("card card-body mt-0 pt-0 pr-0 pl-0") {
        div("row mt-0 ml-0 mr-0") {
            div("input-group") {
                input(type = InputType.text, classes = "form-control") {
                    attrs.id = "input-users-to-add"
                    attrs.list = "complete-users-to-add"
                    attrs.placeholder = "username"
                    attrs.value = userToAdd.name
                    attrs.onChangeFunction = {
                        setUserToAdd(UserInfo((it.target as HTMLInputElement).value))
                        getUsersNotFromGroup()
                    }
                }
                datalist {
                    attrs.id = "complete-users-to-add"
                    attrs["style"] = jso<CSSProperties> {
                        appearance = None.none
                    }
                    for (user in usersNotFromGroup) {
                        option {
                            attrs.value = user.name
                            attrs.label = user.source ?: ""
                        }
                    }
                }
                div("input-group-append") {
                    button(type = ButtonType.button, classes = "btn btn-sm btn-success") {
                        attrs.onClickFunction = {
                            addUserToGroup()
                        }
                        +"Add user"
                    }
                }
            }
        }
        for (user in usersFromGroup) {
            val userName = user.source + ":" + user.name
            val userRole = getUserGroups(user)[props.groupPath] ?: Role.VIEWER
            val userIndex = usersFromGroup.indexOf(user)
            div("row mt-2 mr-0") {
                div("col-1") {
                    button(classes = "btn h-auto w-auto") {
                        fontAwesomeIcon(icon = faTimesCircle)
                        attrs.id = "remove-user-$userIndex"
                        attrs.hidden = selfRole.priority <= userRole.priority
                        attrs.onClick = {
                            val deletedUserIndex = attrs.id.split("-")[2].toInt()
                            setUserToDelete(usersFromGroup[deletedUserIndex])
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
                            setRoleChange { SetRoleRequest(userName.split(":")[1], target.value.toRole()) }
                            updatePermissions()
                        }
                        attrs.id = "role-$userIndex"
                        for (role in Role.values()) {
                            if (role != Role.NONE && (role.priority < selfRole.priority || role == userRole)) {
                                option {
                                    attrs.value = role.formattedName
                                    attrs.selected = role == userRole
                                    +role.toString()
                                }
                            }
                        }
                        attrs.disabled = userRole.priority >= selfRole.priority
                    }
                }
            }
        }
    }
}
