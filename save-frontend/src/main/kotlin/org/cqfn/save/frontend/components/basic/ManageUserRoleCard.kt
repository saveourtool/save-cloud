/**
 * Components for cards
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "MatchingDeclarationName")

package org.cqfn.save.frontend.components.basic

import csstype.None
import kotlinx.html.*
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon

import react.PropsWithChildren
import react.fc

import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.js.jso
import org.cqfn.save.domain.Role
import org.cqfn.save.frontend.externals.fontawesome.faTimesCircle
import org.cqfn.save.frontend.utils.useRequest
import org.cqfn.save.info.UserInfo
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import react.CSSProperties
import react.dom.*
import react.dom.onClick
import react.useState

private fun String.toRole() = when (this) {
    Role.VIEWER.toString(), Role.VIEWER.formattedName -> Role.VIEWER
    Role.SUPER_ADMIN.toString(), Role.SUPER_ADMIN.formattedName -> Role.SUPER_ADMIN
    Role.OWNER.toString(), Role.OWNER.formattedName -> Role.OWNER
    Role.ADMIN.toString(), Role.ADMIN.formattedName -> Role.ADMIN
    Role.NONE.toString(), Role.NONE.formattedName -> Role.NONE
    else -> throw IllegalStateException("Unknown role is passed: $this")
}

/**
 * [RProps] for card component
 */
external interface ManageUserRoleCardProps : PropsWithChildren {
    var selfUserInfo: UserInfo
    var usersFromGroup: List<UserInfo>
    var usersNotFromGroup: List<UserInfo>
}

/**
 * A functional `RComponent` for a card.
 *
 * @param contentBuilder a builder function for card content
 * @param isBordered - adds a border to the card
 * @param hasBg - adds a white background
 * @return a functional component representing a card
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
fun manageUserRoleCardComponent(
    addUserToGroup: (userToAdd: UserInfo) -> Unit,
    deleteUser: (userToDelete: UserInfo) -> Unit,
    getUserGroups: (UserInfo) -> Map<String, Role>,
) = fc<ManageUserRoleCardProps> { props ->
    val (userToAdd, setUserToAdd) = useState(UserInfo(""))
    val (userToDelete, setUserToDelete) = useState(UserInfo(""))

    val (permissionsChanged, setPermissionsChanged) = useState(mapOf<String, Role>())

    val groupName: String = ""
    val selfRole: Role = getUserGroups(props.selfUserInfo)[groupName] ?: Role.NONE

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
                    }
                }
                datalist {
                    attrs.id = "complete-users-to-add"
                    attrs["style"] = jso<CSSProperties> {
                        appearance = None.none
                    }
                    for (user in props.usersNotFromGroup) {
                        option {
                            attrs.value = user.name
                            attrs.label = user.source ?: ""
                        }
                    }
                }
                div("input-group-append") {
                    button(type = ButtonType.button, classes = "btn btn-sm btn-success") {
                        attrs.onClickFunction = {
                            addUserToGroup(userToAdd)
                        }
                        +"Add user"
                    }
                }
            }
        }
        for (user in props.usersFromGroup) {
            val userName = user.source + ":" + user.name
            val userRole = getUserGroups(user)[groupName] ?: Role.VIEWER
            val userIndex = props.usersFromGroup.indexOf(user)
            div("row mt-2 mr-0") {
                div("col-1") {
                    button(classes = "btn h-auto w-auto") {
                        fontAwesomeIcon(icon = faTimesCircle)
                        attrs.id = "remove-user-$userIndex"
                        attrs.hidden = selfRole.priority <= userRole.priority
                        attrs.onClick = {
                            val deletedUserIndex = attrs.id.split("-")[2].toInt()
                            setUserToDelete(props.usersFromGroup[deletedUserIndex])
                            deleteUser(userToDelete)
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
                                        user.name == props.selfUserInfo.name && selfRole == role)) {
                                option {
                                    attrs.value = role.formattedName
                                    attrs.selected = role == userRole
                                    +role.toString()
                                }
                            }
                        }
                        attrs.disabled = (permissionsChanged[userName] ?: userRole).priority >= selfRole.priority
                    }
                }
            }
        }
    }
}
