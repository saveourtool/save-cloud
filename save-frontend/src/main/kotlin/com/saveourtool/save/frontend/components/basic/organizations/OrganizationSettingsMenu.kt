@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OrganizationDto
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.frontend.components.basic.manageUserRoleCardComponent
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.isSuperAdmin
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import org.w3c.fetch.Response
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.router.useNavigate
import web.cssom.ClassName
import web.html.InputType

private val organizationGitCredentialsManageCard = manageGitCredentialsCardComponent()

/**
 * SETTINGS tab in OrganizationView
 */
val organizationSettingsMenu = organizationSettingsMenu()

/**
 * OrganizationSettingsMenu component props
 */
external interface OrganizationSettingsMenuProps : Props {
    /**
     * Current organization settings
     */
    var organizationName: String

    /**
     * Information about current user
     */
    var currentUserInfo: UserInfo

    /**
     * [Role] of user that is observing this component
     */
    var selfRole: Role

    /**
     * Callback to show error message
     */
    @Suppress("TYPE_ALIAS")
    var updateErrorMessage: (Response, String) -> Unit

    /**
     * Callback to show notification message
     */
    var updateNotificationMessage: (String, String) -> Unit

    /**
     * Current organization
     */
    var organization: OrganizationDto

    /**
     * Callback invoked in order to change canCreateContests flag
     */
    var onCanCreateContestsChange: (Boolean) -> Unit
}

/**
 * Makes a call to change project status
 *
 * @param organizationName name of the organization whose status will be changed
 * @param status is new status
 * @return lazy response
 */
fun responseChangeOrganizationStatus(organizationName: String, status: OrganizationStatus): suspend WithRequestStatusContext.() -> Response = {
    post(
        url = "$apiUrl/organizations/$organizationName/change-status?status=$status",
        headers = jsonHeaders,
        body = undefined,
        loadingHandler = ::noopLoadingHandler,
        responseHandler = ::noopResponseHandler,
    )
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "ComplexMethod"
)
private fun organizationSettingsMenu() = FC<OrganizationSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val organizationPath = props.organizationName

    val navigate = useNavigate()
    div {
        className = ClassName("row justify-content-center mb-2 text-gray-900")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Users"
            }
            manageUserRoleCardComponent {
                selfUserInfo = props.currentUserInfo
                groupPath = organizationPath
                groupType = "organization"
                getUserGroups = { it.organizations }
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Git credentials"
            }
            organizationGitCredentialsManageCard {
                selfUserInfo = props.currentUserInfo
                organizationName = props.organizationName
                updateErrorMessage = props.updateErrorMessage
            }
        }
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Main settings"
            }
            div {
                className = ClassName("card card-body mt-0 p-0")
                if (props.selfRole.isSuperAdmin()) {
                    div {
                        className = ClassName("d-sm-flex justify-content-center form-check pl-3 pr-3 pt-3")
                        div {
                            input {
                                className = ClassName("form-check-input")
                                type = InputType.checkbox
                                value = props.organization.canCreateContests.toString()
                                id = "canCreateContestsCheckbox"
                                checked = props.organization.canCreateContests
                                onChange = {
                                    props.onCanCreateContestsChange(!props.organization.canCreateContests)
                                }
                            }
                        }
                        div {
                            label {
                                className = ClassName("form-check-label")
                                htmlFor = "canCreateContestsCheckbox"
                                +"Can create contests"
                            }
                        }
                    }
                }
                div {
                    className = ClassName("d-sm-flex align-items-center justify-content-center p-3")
                    actionButton {
                        title = "WARNING: You are about to delete this organization"
                        errorTitle = "You cannot delete the organization ${props.organizationName}"
                        message = "Are you sure you want to delete the organization ${props.organizationName}?"
                        clickMessage = "Also ban this organization"
                        buttonStyleBuilder = { childrenBuilder ->
                            with(childrenBuilder) {
                                +"Delete ${props.organizationName}"
                            }
                        }
                        classes = "btn btn-sm btn-outline-danger"
                        modalButtons = { action, closeWindow, childrenBuilder, isClickMode ->
                            val actionName = if (isClickMode) "ban" else "delete"
                            with(childrenBuilder) {
                                buttonBuilder(label = "Yes, $actionName ${props.organizationName}", style = "danger", classes = "mr-2") {
                                    action()
                                    closeWindow()
                                }
                                buttonBuilder("Cancel") {
                                    closeWindow()
                                }
                            }
                        }
                        onActionSuccess = { _ ->
                            navigate(to = "/${FrontendRoutes.PROJECTS}")
                        }
                        conditionClick = props.currentUserInfo.isSuperAdmin()
                        sendRequest = { isBanned ->
                            val newStatus = if (isBanned) OrganizationStatus.BANNED else OrganizationStatus.DELETED
                            responseChangeOrganizationStatus(props.organizationName, newStatus)
                        }
                    }
                }
            }
        }
    }
}
