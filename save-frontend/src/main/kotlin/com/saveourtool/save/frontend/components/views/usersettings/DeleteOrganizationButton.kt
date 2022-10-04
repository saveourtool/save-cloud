/**
 * Utilities for cli args parsing
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import csstype.ClassName
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
    val displayButtonEntry = {}
    val windowOpenness = useWindowOpenness()
    val (displayTitle, putDisplayTitle) = useState<String?>(null)
    val (displayMessage, putDisplayMessage) = useState<String?>(null)
    val (displayButton, putDisplayButtons) = useState(displayButtonEntry)

    val displayButtonErrors = {
        buttonBuilder("Ok") {
            windowOpenness.closeWindow()
        }
    }


    val deleteOrganization = useDeferredRequest {
        val responseFromDeleteOrganization =
                delete(
                    "$apiUrl/organizations/${props.organizationName}/delete",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                    errorHandler = ::noopResponseHandler,
                )
        if (responseFromDeleteOrganization.ok) {
            props.onDeletionSuccess()
        } else {
            putDisplayTitle("You cannot delete ${props.organizationName}")
            putDisplayMessage(responseFromDeleteOrganization.unpackMessage())
            putDisplayButtons(displayButtonErrors)
            windowOpenness.openWindow()
        }
    }

    displayModal(
        isOpen = windowOpenness.isOpen(),
        title = displayTitle ?: "",
        message = displayMessage ?: "",
        onCloseButtonPressed = windowOpenness.closeWindowAction()
    ) {
        displayButton
    }

    val displayButtonDelete = {
        buttonBuilder("Yes, delete ${props.organizationName}", "danger") {
            deleteOrganization()
            windowOpenness.closeWindow()
        }
        buttonBuilder("Cancel") {
            windowOpenness.closeWindow()
        }
    }

    div {
        button {
            className = ClassName(props.classes)
            props.buttonStyleBuilder(this)
            id = "remove-organization-${props.organizationName}"
            onClick = {
                putDisplayTitle("Warning: deletion of organization")
                putDisplayMessage("You are about to delete organization ${props.organizationName}. Are you sure?")
                putDisplayButtons(displayButtonDelete)
                windowOpenness.openWindow()
                //windowOpenness.openWindow()
            }
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
     * classname for the button
     */
    var classes: String
}
