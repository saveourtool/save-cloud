@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.domain.Role
import com.saveourtool.save.frontend.components.basic.manageUserRoleCardComponent
import com.saveourtool.save.info.UserInfo
import csstype.ClassName

import org.w3c.fetch.Response
import react.*
import react.dom.*

import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div

private val organizationPermissionManagerCard = manageUserRoleCardComponent()

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
     * Callback to delete organization
     */
    var deleteOrganizationCallback: () -> Unit

    /**
     * Callback to show error message
     */
    var updateErrorMessage: (Response) -> Unit

    /**
     * Callback to show notification message
     */
    var updateNotificationMessage: (String, String) -> Unit
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
    val (wasConfirmationModalShown, setWasConfirmationModalShown) = useState(false)
    div {
        className = ClassName("row justify-content-center mb-2")
        // ===================== LEFT COLUMN =======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Users"
            }
            organizationPermissionManagerCard {
                selfUserInfo = props.currentUserInfo
                groupPath = organizationPath
                groupType = "organization"
                this.wasConfirmationModalShown = wasConfirmationModalShown
                updateErrorMessage = props.updateErrorMessage
                getUserGroups = { it.organizations }
                showGlobalRoleWarning = {
                    props.updateNotificationMessage(
                        "Super admin message",
                        "Keep in mind that you are super admin, so you are able to manage organization regardless of your organization permissions.",
                    )
                    setWasConfirmationModalShown(true)
                }
            }
        }
        // ===================== RIGHT COLUMN ======================================================================
        div {
            className = ClassName("col-4 mb-2 pl-0 pr-0 mr-2 ml-2")
            div {
                className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                +"Main settings"
            }
            div {
                className = ClassName("card card-body mt-0 pt-0 pr-0 pl-0")
                div {
                    className = ClassName("row d-flex justify-content-center mt-3")
                    div {
                        className = ClassName("col-3 d-sm-flex align-items-center justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-sm btn-danger")
                            disabled = !props.selfRole.hasDeletePermission()
                            onClick = {
                                props.deleteOrganizationCallback()
                            }
                            +"Delete organization"
                        }
                    }
                }
            }
        }
    }
}
