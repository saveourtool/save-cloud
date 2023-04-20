/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.Role
import com.saveourtool.save.domain.Role.OWNER
import com.saveourtool.save.frontend.components.inputform.inputWithDebounceForUserInfo
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.permission.SetRoleRequest
import com.saveourtool.save.utils.getHighestRole

import csstype.ClassName
import react.*
import web.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val manageUserRoleCardComponent = manageUserRoleCardComponent()

/**
 * [Props] for card component
 */
external interface ManageUserRoleCardProps : Props {
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

    /**
     * Lambda to get users from project/organization
     */
    var getUserGroups: (UserInfo) -> Map<String, Role>
}

/**
 * A functional `Component` for a card that shows users from the group and their permissions.
 *
 * @return a functional component representing a role managing card
 */
@Suppress(
    "LongMethod",
    "EMPTY_BLOCK_STRUCTURE_ERROR",
    "TOO_LONG_FUNCTION",
    "MAGIC_NUMBER",
    "ComplexMethod",
)
private fun manageUserRoleCardComponent() = FC<ManageUserRoleCardProps> { props ->
    val (usersFromGroup, setUsersFromGroup) = useState(emptyList<UserInfo>())
    val getUsersFromGroup = useDeferredRequest {
        val usersFromDb = get(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<List<UserInfo>>()
            }
        setUsersFromGroup(usersFromDb)
    }

    val (roleChange, setRoleChange) = useState(SetRoleRequest("", Role.NONE))
    val updatePermissions = useDeferredRequest {
        val response = post(
            "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles",
            jsonHeaders,
            Json.encodeToString(roleChange),
            loadingHandler = ::noopLoadingHandler,
        )
        if (response.ok) {
            getUsersFromGroup()
        }
    }

    val (userToAdd, setUserToAdd) = useState(UserInfo(""))
    val addUserToGroup = useDeferredRequest {
        val response = post(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles",
            headers = jsonHeaders,
            body = Json.encodeToString(SetRoleRequest(userToAdd.name, Role.VIEWER)),
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            setUserToAdd(UserInfo(""))
            getUsersFromGroup()
        }
    }

    val (userToDelete, setUserToDelete) = useState(UserInfo(""))
    val deleteUser = useDeferredRequest {
        val response = delete(
            url = "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles/${userToDelete.name}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
        if (response.ok) {
            getUsersFromGroup()
        }
    }

    val (selfRole, setSelfRole) = useState(Role.NONE)
    useRequest {
        val role = get(
            "$apiUrl/${props.groupType}s/${props.groupPath}/users/roles",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString<String>()
            }
            .toRole()
        if (role.isLowerThan(OWNER) && props.selfUserInfo.isSuperAdmin()) {
            showGlobalRoleConfirmation()
        }
        setSelfRole(getHighestRole(role, props.selfUserInfo.globalRole))
    }

    useOnce {
        getUsersFromGroup()
    }

    div {
        className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0")
        div {
            className = ClassName("row mt-0 ml-0 mr-0 shadow-sm rounded")
            div {
                className = ClassName("input-group")
                inputWithDebounceForUserInfo {
                    getOptionFromString = { UserInfo(it) }
                    selectedOption = userToAdd
                    setSelectedOption = { setUserToAdd(it) }
                    getUrlForOptions = { prefix -> "$apiUrl/${props.groupType}s/${props.groupPath}/users/not-from?prefix=$prefix" }
                    getString = { it.name }
                    placeholder = "username"
                    decodeListFromJsonString = { it.decodeFromJsonString() }
                    getHTMLDataListElementFromOption = { childrenBuilder, userInfo ->
                        with(childrenBuilder) {
                            option {
                                value = userInfo.name
                                label = userInfo.source
                            }
                        }
                    }
                }
                div {
                    className = ClassName("input-group-append")
                    button {
                        type = ButtonType.button
                        className = ClassName("btn btn-sm btn-outline-success")
                        onClick = {
                            addUserToGroup()
                        }
                        disabled = userToAdd.name.isBlank()
                        +"Add user"
                    }
                }
            }
        }
        for (user in usersFromGroup) {
            val userName = user.name
            val userRole = props.getUserGroups(user)[props.groupPath] ?: Role.VIEWER
            val userIndex = usersFromGroup.indexOf(user)
            div {
                className = ClassName("row mt-2 mr-0 justify-content-between align-items-center")
                div {
                    className = ClassName("col-7 d-flex justify-content-start align-items-center")
                    div {
                        className = ClassName("col-2 align-items-center")
                        fontAwesomeIcon(
                            when (user.source) {
                                "github" -> faGithub
                                "codehub" -> faCopyright
                                else -> faHome
                            },
                            classes = "h-75 w-75"
                        )
                    }
                    div {
                        className = ClassName("col-7 text-left align-self-center pl-0")
                        +userName
                    }
                }
                div {
                    className = ClassName("col-5 align-self-right d-flex align-items-center justify-content-end")
                    button {
                        type = ButtonType.button
                        className = ClassName("btn col-2 align-items-center mr-2")
                        fontAwesomeIcon(icon = faTimesCircle)
                        val canDelete = selfRole.isSuperAdmin() ||
                                selfRole == OWNER && !isSelfRecord(props.selfUserInfo, user) ||
                                userRole.isLowerThan(selfRole)
                        id = "remove-user-$userIndex"
                        hidden = !canDelete
                        onClick = {
                            setUserToDelete(usersFromGroup[userIndex])
                            deleteUser()
                        }
                    }
                    select {
                        className = ClassName("custom-select col-9")
                        onChange = { event ->
                            setRoleChange { SetRoleRequest(userName, event.target.value.toRole()) }
                            updatePermissions()
                        }
                        value = userRole.formattedName
                        id = "role-$userIndex"
                        rolesAssignableBy(selfRole)
                            .sortedByDescending {
                                it.priority
                            }
                            .map {
                                option {
                                    value = it.formattedName
                                    +it.formattedName
                                }
                            }
                        disabled = (selfRole == OWNER && isSelfRecord(props.selfUserInfo, user)) ||
                                !(selfRole.isHigherOrEqualThan(OWNER) || userRole.isLowerThan(selfRole))
                    }
                }
            }
        }
    }
}

private fun isSelfRecord(selfUserInfo: UserInfo, otherUserInfo: UserInfo) = otherUserInfo.name == selfUserInfo.name

private fun rolesAssignableBy(role: Role) = Role.values()
    .filter { it != Role.NONE }
    .filterNot(Role::isSuperAdmin)
    .filter { role == OWNER || it.isLowerThan(role) || role == it }
