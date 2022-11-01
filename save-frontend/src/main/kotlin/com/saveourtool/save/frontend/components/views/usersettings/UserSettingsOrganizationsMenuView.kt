package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.utils.getHighestRole
import com.saveourtool.save.v1

import csstype.BorderRadius
import csstype.ClassName
import org.w3c.fetch.Response
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul

import kotlinx.js.jso

@Suppress("MISSING_KDOC_TOP_LEVEL", "TOO_LONG_FUNCTION", "LongMethod")
class UserSettingsOrganizationsMenuView : UserSettingsView() {
    private val organizationListCard = cardComponent(isBordered = false, hasBg = true)

    @Suppress("ComplexMethod")
    override fun renderMenu(): FC<UserSettingsProps> = FC { props ->
        organizationListCard {
            div {
                className = ClassName("d-sm-flex align-items-center justify-content-center mb-4 mt-4")
                h1 {
                    className = ClassName("h3 mb-0 mt-2 text-gray-800")
                    +"Organizations"
                }
            }

            ul {
                className = ClassName("list-group list-group-flush")
                state.selfOrganizationDtos.forEach { organizationDto ->
                    li {
                        className = ClassName("list-group-item")
                        div {
                            className = ClassName("row justify-content-between align-items-center")
                            div {
                                className = ClassName("align-items-center ml-3")
                                img {
                                    className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                                    src = organizationDto.avatar?.let {
                                        "/api/$v1/avatar$it"
                                    } ?: "img/company.svg"
                                    height = 60.0
                                    width = 60.0
                                }
                                a {
                                    className = ClassName("ml-2")
                                    href = "#/${organizationDto.name}"
                                    +organizationDto.name
                                }
                            }
                            div {
                                className = ClassName("col-5 align-self-right d-flex align-items-center justify-content-end")
                                val role = state.userInfo?.name?.let { organizationDto.userRoles[it] } ?: Role.NONE
                                val highestLocalRole = getHighestRole(role, state.userInfo?.globalRole)
                                if (highestLocalRole.isHigherOrEqualThan(Role.OWNER)) {
                                    actionButton {
                                        title = "WARNING: You want to delete an organization"
                                        errorTitle = "You cannot delete ${organizationDto.name}"
                                        message = "Are you sure you want to delete a ${organizationDto.name}?"
                                        clickMessage = "Change to ban mode"
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(icon = faTrashAlt)
                                            }
                                        }
                                        classes = "btn mr-3"
                                        modalButtons = { action, window, childrenBuilder ->
                                            with(childrenBuilder) {
                                                buttonBuilder(label = "Yes, delete ${organizationDto.name}", style = "danger", classes = "mr-2") {
                                                    action()
                                                    window.closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    window.closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = { clickMode: Boolean ->
                                            setState {
                                                selfOrganizationDtos = selfOrganizationDtos.minus(organizationDto)
                                                if (clickMode) {
                                                    selfBannedOrganizationDtos = selfBannedOrganizationDtos.plus(organizationDto)
                                                } else {
                                                    selfDeletedOrganizationDtos = selfDeletedOrganizationDtos.plus(organizationDto)
                                                }
                                            }
                                        }
                                        conditionClick = highestLocalRole.isHigherOrEqualThan(Role.SUPER_ADMIN)
                                        sendRequest = { isClickMode ->
                                            responseDeleteOrganization(isClickMode, organizationDto.name)
                                        }
                                    }
                                }
                                div {
                                    className = ClassName("mr-3")
                                    +role.formattedName
                                }
                            }
                        }
                    }
                }

                state.selfDeletedOrganizationDtos.forEach { organizationDto ->
                    li {
                        className = ClassName("list-group-item")
                        div {
                            className = ClassName("row justify-content-between align-items-center")
                            div {
                                className = ClassName("align-items-center ml-3")
                                img {
                                    className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle mr-2")
                                    src = organizationDto.avatar?.let {
                                        "/api/$v1/avatar$it"
                                    } ?: "img/company.svg"
                                    height = 60.0
                                    width = 60.0
                                }
                                +organizationDto.name
                                span {
                                    className = ClassName("border ml-2 pr-1 pl-1 text-xs text-muted ")
                                    style = jso { borderRadius = "2em".unsafeCast<BorderRadius>() }
                                    +"deleted"
                                }
                            }
                            div {
                                className = ClassName("col-5 align-self-right d-flex align-items-center justify-content-end")
                                val role = state.userInfo?.name?.let { organizationDto.userRoles[it] } ?: Role.NONE
                                if (getHighestRole(role, state.userInfo?.globalRole).isHigherOrEqualThan(Role.OWNER)) {
                                    actionButton {
                                        title = "WARNING: You want to recover an organization"
                                        errorTitle = "You cannot recover ${organizationDto.name}"
                                        message = "Are you sure you want to recover an organization ${organizationDto.name}?"
                                        buttonStyleBuilder = { childrenBuilder ->
                                            with(childrenBuilder) {
                                                fontAwesomeIcon(icon = faRedo)
                                            }
                                        }
                                        classes = "btn mr-3"
                                        modalButtons = { action, window, childrenBuilder ->
                                            with(childrenBuilder) {
                                                buttonBuilder(label = "Yes, recover ${organizationDto.name}", style = "warning", classes = "mr-2") {
                                                    action()
                                                    window.closeWindow()
                                                }
                                                buttonBuilder("Cancel") {
                                                    window.closeWindow()
                                                }
                                            }
                                        }
                                        onActionSuccess = {
                                            setState {
                                                selfDeletedOrganizationDtos = selfDeletedOrganizationDtos.minus(organizationDto)
                                                selfOrganizationDtos = selfOrganizationDtos.plus(organizationDto)
                                            }
                                        }
                                        conditionClick = false
                                        sendRequest = { _ ->
                                            responseRecoverOrganization(organizationDto.name)
                                        }
                                    }
                                }
                                div {
                                    className = ClassName("mr-3")
                                    +role.formattedName
                                }
                            }
                        }
                    }
                }
                state.selfBannedOrganizationDtos.forEach { organizationDto ->
                    li {
                        className = ClassName("list-group-item")
                        div {
                            className = ClassName("row justify-content-between align-items-center")
                            div {
                                className = ClassName("align-items-center ml-3 text-danger")
                                img {
                                    className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle mr-2 ")
                                    src = organizationDto.avatar?.let {
                                        "/api/$v1/avatar$it"
                                    } ?: "img/company.svg"
                                    height = 60.0
                                    width = 60.0
                                }
                                +organizationDto.name
                                span {
                                    className = ClassName("border ml-2 pr-1 pl-1 text-xs text-muted ")
                                    style = jso { borderRadius = "2em".unsafeCast<BorderRadius>() }
                                    +"banned"
                                }
                            }
                            div {
                                className = ClassName("col-5 align-self-right d-flex align-items-center justify-content-end")
                                val role = state.userInfo?.name?.let { organizationDto.userRoles[it] } ?: Role.NONE
                                div {
                                    className = ClassName("mr-3")
                                    +role.formattedName
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Makes a call to delete or ban the organization, depending on the [isClickMode] value
 *
 * @param isClickMode
 * @param organizationName
 * @return response
 */
fun responseDeleteOrganization(isClickMode: Boolean, organizationName: String): suspend WithRequestStatusContext.() -> Response = {
    delete(
        url = buildString {
            append("$apiUrl/organizations/$organizationName/delete")
            if (isClickMode) append("?status=${OrganizationStatus.BANNED}") else append("?status=${OrganizationStatus.DELETED}")
        },
        headers = jsonHeaders,
        loadingHandler = ::noopLoadingHandler,
        errorHandler = ::noopResponseHandler,
    )
}

/**
 * Makes a call to recover the organization
 *
 * @param organizationName
 * @return response
 */
fun responseRecoverOrganization(organizationName: String): suspend WithRequestStatusContext.() -> Response = {
    post(
        url = "$apiUrl/organizations/$organizationName/recover",
        headers = jsonHeaders,
        body = undefined,
        loadingHandler = ::noopLoadingHandler,
        responseHandler = ::noopResponseHandler,
    )
}
