@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
)

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.modal.ModalDialog
import com.saveourtool.save.frontend.components.modal.ModalDialogStrings
import csstype.ClassName
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button

/**
 * When clicked, runs [DeleteButtonProps.action] after user confirmation is
 * received.
 * The confirmation title and text can be specified via
 * [DeleteButtonProps.confirmDialog].
 *
 * @see DeleteButtonProps
 */
internal val deleteButton: FC<DeleteButtonProps> = FC { props ->
    val confirmDialogWindow = useWindowOpenness()

    button {
        type = ButtonType.button
        id = props.id
        className = ClassName(props.classes.joinToString(separator = " "))
        title = props.tooltipText
        props.children(this)
        onClick = {
            confirmDialogWindow.openWindow()
        }
    }

    runWithConfirmation {
        confirmDialog = ModalDialog(props.confirmDialog, confirmDialogWindow)
        action = props.action
    }
}

/**
 * The properties of the [deleteButton] functional component.
 *
 * @see deleteButton
 */
internal external interface DeleteButtonProps : Props {
    /**
     * The `id` HTML attribute.
     */
    var id: String

    /**
     * The list of CSS classes.
     */
    var classes: List<String>

    /**
     * The tooltip text of this button.
     */
    var tooltipText: String

    /**
     * The child elements of this button.
     */
    var children: (ChildrenBuilder) -> Unit

    /**
     * The confirmation dialog window to show when this button is clicked.
     */
    var confirmDialog: ModalDialogStrings

    /**
     * The action to invoke once user confirmation is received.
     */
    var action: DeferredRequestAction<Unit>
}
