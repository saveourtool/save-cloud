package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.entities.OrganizationWithUsers
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.basic.organizations.responseChangeOrganizationStatus
import com.saveourtool.save.frontend.components.views.actionButtonClasses
import com.saveourtool.save.frontend.components.views.actionIconClasses
import com.saveourtool.save.frontend.externals.fontawesome.faRedo
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.actionButton
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.frontend.utils.spanWithClassesAndText
import com.saveourtool.save.v1

import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import web.cssom.ClassName

@Suppress("MISSING_KDOC_TOP_LEVEL", "TOO_LONG_FUNCTION", "LongMethod")
class UserSettingsOrganizationsMenuView : UserSettingsView() {
    private val organizationListCard = cardComponent(isBordered = false, hasBg = true)
    private val comparator: Comparator<OrganizationWithUsers> =
            compareBy<OrganizationWithUsers> { it.organization.status.ordinal }
                .thenBy { it.organization.name }

    /**
     * Removes [oldOrganizationWithUsers] by [selfOrganizationWithUserList], adds [newOrganizationWithUsers] in [selfOrganizationWithUserList]
     * and sorts the resulting list by their status and then by name
     *
     * @param organizationWithUsers
     * @param newStatus
     */
    private fun updateOrganizationWithUserInOrganizationWithUsersList(oldOrganizationWithUsers: OrganizationWithUsers, newOrganizationWithUsers: OrganizationWithUsers) {
        setState {
            selfOrganizationWithUserList = selfOrganizationWithUserList.minusElement(oldOrganizationWithUsers)
                .plusElement(newOrganizationWithUsers)
                .sortedWith(comparator)
        }
    }

    /**
     * Returned the [organizationWithUsers] with the updated [OrganizationStatus] field to the [newStatus] in the organization field
     */
    private fun changeOrganizationWithUserStatus(organizationWithUsers: OrganizationWithUsers, newStatus: OrganizationStatus) =
            organizationWithUsers.copy(organization = organizationWithUsers.organization.copy(status = newStatus))

    @Suppress("CyclomaticComplexMethod")
    override fun renderMenu(): VFC = VFC {
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
                state.selfOrganizationWithUserList.forEach { organizationWithUsers ->
                    val organizationDto = organizationWithUsers.organization
                    li {
                        className = ClassName("list-group-item")
                        div {
                            className = ClassName("row justify-content-between align-items-center")
                            div {
                                val textClassName = when (organizationDto.status) {
                                    OrganizationStatus.CREATED -> "text-primary"
                                    OrganizationStatus.DELETED -> "text-secondary"
                                    OrganizationStatus.BANNED -> "text-danger"
                                }
                                className = ClassName("align-items-center ml-3 $textClassName")
                                img {
                                    className = ClassName("avatar avatar-user width-full border color-bg-default rounded-circle mr-2")
                                    src = organizationDto.avatar?.let {
                                        "/api/$v1/avatar$it"
                                    } ?: "img/company.svg"
                                    height = 60.0
                                    width = 60.0
                                }
                                when (organizationDto.status) {
                                    OrganizationStatus.CREATED -> a {
                                        href = "#/${organizationDto.name}"
                                        +organizationDto.name
                                    }
                                    OrganizationStatus.DELETED -> {
                                        +organizationDto.name
                                        spanWithClassesAndText("text-secondary", organizationDto.status.name.lowercase())
                                    }
                                    OrganizationStatus.BANNED -> {
                                        +organizationDto.name
                                        spanWithClassesAndText("text-danger", organizationDto.status.name.lowercase())
                                    }
                                }
                            }
                            div {
                                className = ClassName("col-5 align-self-right d-flex align-items-center justify-content-end")
                                val role = state.userInfo?.name?.let { organizationWithUsers.userRoles[it] } ?: Role.NONE
                                if (role.isHigherOrEqualThan(Role.OWNER)) {
                                    when (organizationDto.status) {
                                        OrganizationStatus.CREATED -> actionButton {
                                            title = "WARNING: About to delete this organization..."
                                            errorTitle = "You cannot delete the organization ${organizationDto.name}"
                                            message = "Are you sure you want to delete the organization ${organizationDto.name}?"
                                            buttonStyleBuilder = { childrenBuilder ->
                                                with(childrenBuilder) {
                                                    fontAwesomeIcon(icon = faTrashAlt, classes = actionIconClasses.joinToString(" "))
                                                }
                                            }
                                            classes = actionButtonClasses.joinToString(" ")
                                            modalButtons = { action, closeWindow, childrenBuilder, _ ->
                                                with(childrenBuilder) {
                                                    buttonBuilder(label = "Yes, delete ${organizationDto.name}", style = "danger", classes = "mr-2") {
                                                        action()
                                                        closeWindow()
                                                    }
                                                    buttonBuilder("Cancel") {
                                                        closeWindow()
                                                    }
                                                }
                                            }
                                            onActionSuccess = { _ ->
                                                updateOrganizationWithUserInOrganizationWithUsersList(
                                                    organizationWithUsers,
                                                    changeOrganizationWithUserStatus(organizationWithUsers, OrganizationStatus.DELETED),
                                                )
                                            }
                                            conditionClick = false
                                            sendRequest = { _ ->
                                                responseChangeOrganizationStatus(organizationDto.name, OrganizationStatus.DELETED)
                                            }
                                        }
                                        OrganizationStatus.DELETED -> actionButton {
                                            title = "WARNING: About to recover this organization..."
                                            errorTitle = "You cannot recover the organization ${organizationDto.name}"
                                            message = "Are you sure you want to recover the organization ${organizationDto.name}?"
                                            buttonStyleBuilder = { childrenBuilder ->
                                                with(childrenBuilder) {
                                                    fontAwesomeIcon(icon = faRedo, classes = actionIconClasses.joinToString(" "))
                                                }
                                            }
                                            classes = actionButtonClasses.joinToString(" ")
                                            modalButtons = { action, closeWindow, childrenBuilder, _ ->
                                                with(childrenBuilder) {
                                                    buttonBuilder(label = "Yes, recover ${organizationDto.name}", style = "danger", classes = "mr-2") {
                                                        action()
                                                        closeWindow()
                                                    }
                                                    buttonBuilder("Cancel") {
                                                        closeWindow()
                                                    }
                                                }
                                            }
                                            onActionSuccess = { _ ->
                                                updateOrganizationWithUserInOrganizationWithUsersList(
                                                    organizationWithUsers,
                                                    changeOrganizationWithUserStatus(organizationWithUsers, OrganizationStatus.CREATED),
                                                )
                                            }
                                            conditionClick = false
                                            sendRequest = { _ ->
                                                responseChangeOrganizationStatus(organizationDto.name, OrganizationStatus.CREATED)
                                            }
                                        }
                                        OrganizationStatus.BANNED -> Unit
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
            }
        }
    }
}
