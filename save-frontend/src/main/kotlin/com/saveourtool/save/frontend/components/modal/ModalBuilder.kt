@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.modal

import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.externals.modal.Styles
import csstype.ClassName
import react.ChildrenBuilder
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.span

/**
 * Universal function to create modals with bootstrap styles inside react modals.
 *
 * @param isOpen modal openness indicator - should be in state
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param modalStyle [Styles] that will be applied to react modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.displayModal(
    isOpen: Boolean,
    title: String,
    message: String,
    modalStyle: Styles = mediumTransparentModalStyle,
    onCloseButtonPressed: (() -> Unit)? = null,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = modalStyle
        modalBuilder(title, message, onCloseButtonPressed, buttonBuilder)
    }
}

/**
 * Universal function to create modals with bootstrap styles.
 *
 * @param title title of the modal that will be shown in top-left corner
 * @param message main text that will be shown in the center of modal
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
fun ChildrenBuilder.modalBuilder(
    title: String,
    message: String,
    onCloseButtonPressed: (() -> Unit)?,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    modalBuilder(
        title = title,
        onCloseButtonPressed = onCloseButtonPressed,
        bodyBuilder = {
            h2 {
                className = ClassName("h6 text-gray-800 mb-2")
                +message
            }
        },
        buttonBuilder = buttonBuilder,
    )
}

/**
 * Universal function to create modals with bootstrap styles.
 *
 * @param title title of the modal that will be shown in top-left corner
 * @param onCloseButtonPressed callback that will be applied to `X` button in the top-right corner
 * @param bodyBuilder lambda that generates body of modal
 * @param buttonBuilder lambda that generates several buttons, must contain either [button] or [buttonBuilder]
 */
fun ChildrenBuilder.modalBuilder(
    title: String,
    onCloseButtonPressed: (() -> Unit)?,
    bodyBuilder: ChildrenBuilder.() -> Unit,
    buttonBuilder: ChildrenBuilder.() -> Unit,
) {
    div {
        className = ClassName("modal-dialog")
        div {
            className = ClassName("modal-content")
            div {
                className = ClassName("modal-header")
                h5 {
                    className = ClassName("modal-title")
                    +title
                }
                onCloseButtonPressed?.let {
                    button {
                        type = ButtonType.button
                        className = ClassName("close")
                        asDynamic()["data-dismiss"] = "modal"
                        ariaLabel = "Close"
                        span {
                            fontAwesomeIcon(icon = faTimesCircle)
                            onClick = { onCloseButtonPressed() }
                        }
                    }
                }
            }
            div {
                className = ClassName("modal-body")
                bodyBuilder()
            }
            div {
                className = ClassName("modal-footer")
                buttonBuilder()
            }
        }
    }
}
