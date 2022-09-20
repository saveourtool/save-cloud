package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.frontend.components.basic.cardComponent
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.externals.fontawesome.faTrashAlt
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.v1

import csstype.ClassName
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import react.FC
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.ul
import react.useState

import kotlinx.browser.window
import kotlinx.coroutines.launch

@Suppress("MISSING_KDOC_TOP_LEVEL", "TOO_LONG_FUNCTION")
class UserSettingsOrganizationsMenuView : UserSettingsView() {
    private val organizationListCard = cardComponent(isBordered = false, hasBg = true)

    override fun renderMenu(): FC<UserSettingsProps> = FC { _ ->
        val (deleteOrganization, setDeleteOrganization) = useState(OrganizationDto("alex"))
        val (isDeleteOrganization, setFlagDeleteOrganization) = useState(false)
        displayModal(
            isDeleteOrganization,
            "Deletion of git credential",
            "Please confirm deletion of ${deleteOrganization.name}. " +
                    "Note! This action deletes all the projects of this organization and the organization itself!",
            mediumTransparentModalStyle,
            { setFlagDeleteOrganization(false) },
        ) {
            buttonBuilder("Ok") {
                deleteOrganization(deleteOrganization)
                setFlagDeleteOrganization(false)
            }
            buttonBuilder("Close", "secondary") {
                setFlagDeleteOrganization(false)
            }
        }
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
                                    className =
                                            ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
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
                                div {
                                    className = ClassName("mr-3")
                                    role
                                    +role.formattedName
                                }
                                if (role.isHigherOrEqualThan(Role.OWNER)) {
                                    div {
                                        button {
                                            className = ClassName("btn mr-3")
                                            fontAwesomeIcon(icon = faTrashAlt)
                                            id = "remove-organization-${organizationDto.name}"
                                            onClick = {
                                                setDeleteOrganization(organizationDto)
                                                setFlagDeleteOrganization(true)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun deleteOrganization(organizationDto: OrganizationDto) {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        lateinit var responseFromDeleteOrganization: Response
        scope.launch {
            responseFromDeleteOrganization =
                    delete(
                        "$apiUrl/organizations/${organizationDto.name}/delete",
                        headers,
                        body = undefined,
                        loadingHandler = ::noopLoadingHandler,
                    )
        }.invokeOnCompletion {
            if (responseFromDeleteOrganization.ok) {
                window.location.reload()
            }
        }
    }
}
