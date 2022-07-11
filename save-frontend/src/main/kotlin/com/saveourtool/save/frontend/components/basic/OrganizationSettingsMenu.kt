@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic

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
}

/**
 * @param deleteOrganizationCallback
 * @param updateErrorMessage
 * @param updateNotificationMessage
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
    updateErrorMsg: (Response) -> Unit,
    updateNotificationMessage: (String, String) -> Unit,
) = FC<OrganizationSettingsMenuProps> { props ->
    @Suppress("LOCAL_VARIABLE_EARLY_DECLARATION")
    val organizationPath = props.organizationName
    val (wasModalShown, setWasModalShown) = useState(false)
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
                wasConfirmationModalShown = wasModalShown
                updateErrorMessage = updateErrorMsg
                getUserGroups = { it.organizations }
                showGlobalRoleWarning = {
                    updateNotificationMessage(
                        "Super admin message",
                        "Keep in mind that you are super admin, so you are able to manage organization regardless of your organization permissions.",
                    )
                    setWasModalShown(true)
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
                            onClick = {
                                deleteOrganizationCallback()
                            }
                            +"Delete organization"
                        }
                    }
                }
            }
        }
    }
}
