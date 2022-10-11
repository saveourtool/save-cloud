/**
 * Utilities for cli args parsing
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.OrganizationStatus
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import csstype.ClassName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.useState

/**
 * Button for delete organization
 *
 * @return noting
 */
val deleteOrganizationButton: FC<DeleteOrganizationButtonProps> = FC { props ->
    val windowOpenness = useWindowOpenness()
    val (displayTitle, setDisplayTitle) = useState("")
    val (displayMessage, setDisplayMessage) = useState("")
    val (modalButtons, setModalButtons) = useState(ModalPurpose.DELETE_MODAL)
    val (statusDeleted, setStatusDeleted) = useState(OrganizationStatus.DELETED)

    val deleteOrganization = useDeferredRequest {
        val responseFromDeleteOrganization =
                delete(
                    "$apiUrl/organizations/${props.organizationName}/delete",
                    headers = jsonHeaders,
                    body = Json.encodeToString(statusDeleted),
                    loadingHandler = ::noopLoadingHandler,
                    errorHandler = ::noopResponseHandler,
                )
        if (responseFromDeleteOrganization.ok) {
            props.onDeletionSuccess()
        } else {
            setDisplayTitle("You cannot delete ${props.organizationName}")
            setDisplayMessage(responseFromDeleteOrganization.unpackMessage())
            setModalButtons(ModalPurpose.ERROR_MODAL)
            windowOpenness.openWindow()
        }
    }

    div {
        button {
            className = ClassName(props.classes)
            props.buttonStyleBuilder(this)
            id = "remove-organization-${props.organizationName}"
            onClick = {
                setDisplayTitle("Warning: deletion of organization")
                setDisplayMessage("You are about to delete organization ${props.organizationName}. Are you sure?")
                setModalButtons(ModalPurpose.DELETE_MODAL)
                windowOpenness.openWindow()
            }
        }
    }

    displayModal(
        isOpen = windowOpenness.isOpen(),
        title = displayTitle,
        message = displayMessage,
        onCloseButtonPressed = windowOpenness.closeWindowAction()
    ) {
        when (modalButtons) {
            ModalPurpose.DELETE_MODAL -> {
                buttonBuilder("Yes, delete ${props.organizationName}", "warning") {
                    deleteOrganization()
                    windowOpenness.closeWindow()
                }
                if (props.userRole.isHigherOrEqualThan(Role.SUPER_ADMIN)) {
                    buttonBuilder("Yes, banned ${props.organizationName}", "danger") {
                        setStatusDeleted(OrganizationStatus.BANNED)
                        deleteOrganization()
                        windowOpenness.closeWindow()
                    }
                }
                buttonBuilder("Cancel") {
                    windowOpenness.closeWindow()
                }
            }
            ModalPurpose.ERROR_MODAL -> buttonBuilder("Ok") { windowOpenness.closeWindow() }
        }
    }
}

/**
 * DeleteOrganizationButton props
 */
external interface DeleteOrganizationButtonProps : Props {
    /**
     * All filters in one class property [organizationName]
     */
    var organizationName: String

    /**
     * lambda to change [organizationName]
     */
    var onDeletionSuccess: () -> Unit

    /**
     * Button View
     */
    var buttonStyleBuilder: (ChildrenBuilder) -> Unit

    /**
     * Classname for the button
     */
    var classes: String

    /**
     * User role
     */
    var userRole: Role
}

private enum class ModalPurpose {
    DELETE_MODAL,
    ERROR_MODAL,
    ;
}
