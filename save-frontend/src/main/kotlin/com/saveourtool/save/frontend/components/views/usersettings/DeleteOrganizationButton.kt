package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.modal.displayConfirmationModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import csstype.ClassName
import react.ChildrenBuilder
import react.FC
import react.PropsWithChildren
import react.dom.html.ReactHTML

/**
 * DeleteOrganizationButton props
 */
external interface DeleteOrganizationButton : PropsWithChildren {
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

/**
 * Button for delete organization
 *
 * @return noting
 */
fun deleteOrganizationButton() = FC<DeleteOrganizationButton> { props ->
    val windowOpenness = useWindowOpenness()

    val deleteOrganization = useDeferredRequest {
        val responseFromDeleteOrganization =
                delete(
                    "$apiUrl/organizations/${props.organizationName}/delete",
                    jsonHeaders,
                    body = undefined,
                    loadingHandler = ::noopLoadingHandler,
                )
        if (responseFromDeleteOrganization.ok) {
            props.onDeletionSuccess()
        }
    }

    displayConfirmationModal(
        windowOpenness,
        "Deletion Organization",
        "Please confirm deletion of ${props.organizationName}. " +
                "Note! This action deletes all the projects of this organization and the organization itself!",
        mediumTransparentModalStyle
    ) {
        deleteOrganization()
    }

    ReactHTML.div {
        ReactHTML.button {
            className = ClassName(props.classes)
            props.buttonStyleBuilder(this)
            id = "remove-organization-${props.organizationName}"
            onClick = {
                windowOpenness.openWindow()
            }
        }
    }
}
