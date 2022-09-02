/**
 * Utilities for kotlin-js ChildrenBuilder
 */

@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import csstype.ClassName
import org.w3c.dom.HTMLButtonElement
import react.ChildrenBuilder
import react.dom.events.MouseEventHandler
import react.dom.html.ReactHTML.button

/**
 * Enum that stores types of confirmation windows for different situations.
 */
enum class ConfirmationType {
    DELETE_CONFIRM,
    DELETE_FILE_CONFIRM,
    NO_BINARY_CONFIRM,
    NO_CONFIRM,
    ;
}

/**
 * Function to create buttons for modals
 *
 * @param label text that will be displayed on button
 * @param style color-defining string
 * @param isDisabled flag that might disable button
 * @param isOutline flag that defines either usual button or outlined will be displayed
 * @param onClickFun button click handler
 */
fun ChildrenBuilder.buttonBuilder(
    label: String,
    style: String = "primary",
    isDisabled: Boolean = false,
    isOutline: Boolean = false,
    onClickFun: MouseEventHandler<HTMLButtonElement>,
) {
    button {
        val outline = if (isOutline) {
            "outline-"
        } else {
            ""
        }
        className = ClassName("btn btn-$outline$style")
        disabled = isDisabled
        onClick = onClickFun
        +label
    }
}
