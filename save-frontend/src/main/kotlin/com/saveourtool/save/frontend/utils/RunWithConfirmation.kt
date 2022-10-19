@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
)

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.components.modal.ModalDialog
import com.saveourtool.save.frontend.components.modal.displayInfoModal
import com.saveourtool.save.frontend.components.modal.displayModal
import react.FC
import react.Props
import react.useState

/**
 * Runs [RunWithConfirmationProps.action] after user confirmation is received.
 * The confirmation title and text can be specified via
 * [RunWithConfirmationProps.confirmDialog].
 *
 * @see RunWithConfirmationProps
 */
internal val runWithConfirmation: FC<RunWithConfirmationProps> = FC { props ->
    require(props.confirmDialog != undefined) {
        "`confirmDialog` is undefined"
    }
    require(props.action != undefined) {
        "`action` is undefined"
    }

    val confirmDialog = props.confirmDialog.window
    val errorDialog = useWindowOpenness()
    val (errorMessage, setErrorMessage) = useState(initialValue = "")

    val action = useDeferredRequest {
        props.action(this) { message ->
            setErrorMessage(message)
            errorDialog.openWindow()
        }
    }

    displayInfoModal(
        errorDialog,
        title = "Error",
        message = errorMessage
    )

    displayModal(
        isOpen = confirmDialog.isOpen(),
        title = props.confirmDialog.strings.title,
        message = props.confirmDialog.strings.message,
        onCloseButtonPressed = confirmDialog.closeWindowAction()
    ) {
        buttonBuilder(label = "Yes", style = "danger") {
            confirmDialog.closeWindow()
            action()
        }

        buttonBuilder(label = "No", style = "secondary") {
            confirmDialog.closeWindow()
        }
    }
}

/**
 * The properties of the [runWithConfirmation] functional component.
 *
 * @see runWithConfirmation
 */
internal external interface RunWithConfirmationProps : Props {
    /**
     * The confirmation dialog window to show before running [action].
     */
    var confirmDialog: ModalDialog

    /**
     * The action to invoke once user confirmation is received.
     */
    var action: DeferredRequestAction<Unit>
}
