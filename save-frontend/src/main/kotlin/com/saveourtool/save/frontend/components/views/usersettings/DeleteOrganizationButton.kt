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

/**
 * Button for delete organization
 *
 * @return noting
 */
val deleteOrganizationButton: FC<DeleteOrganizationButtonProps> = FC { props ->
    val windowOpenness = useWindowOpenness()

    val deleteOrganization = useDeferredRequest {
        val responseFromDeleteOrganization =
                delete(
                    "$apiUrl/organizations/${props.organizationName}/delete",
                    headers = jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                )
        if (responseFromDeleteOrganization.ok) {
            props.onDeletionSuccess()
        }
    }

    displayModal(
        windowOpenness,
        "Warning: deletion of organization",
        "You are about to delete organization ${props.organizationName}. Are you sure?",
    ) {
        buttonBuilder("Yes, delete ${props.organizationName}", "danger") {
            deleteOrganization()
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
                windowOpenness.openWindow()
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
