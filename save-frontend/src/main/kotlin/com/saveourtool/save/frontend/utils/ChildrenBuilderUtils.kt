/**
 * Utilities for kotlin-js ChildrenBuilder
 */

@file:Suppress("FILE_NAME_INCORRECT", "FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.frontend.externals.fontawesome.FontAwesomeIconModule
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon

import react.ChildrenBuilder
import react.dom.events.ChangeEventHandler
import react.dom.events.MouseEventHandler
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import web.cssom.ClassName
import web.html.ButtonType
import web.html.HTMLButtonElement
import web.html.HTMLSelectElement

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
 * Function to create buttons for modals with text as content
 *
 * @param label text that will be displayed on button
 * @param style color-defining string
 * @param isDisabled flag that might disable button
 * @param isOutline flag that defines either usual button or outlined will be displayed
 * @param isActive flag that defines whether button should be displayed as pressed or not
 * @param classes additional classes for button
 * @param title title for tooltip
 * @param onClickFun button click handler
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
fun ChildrenBuilder.buttonBuilder(
    label: String,
    style: String = "primary",
    isDisabled: Boolean = false,
    isOutline: Boolean = true,
    isActive: Boolean = false,
    classes: String = "",
    title: String? = null,
    onClickFun: MouseEventHandler<HTMLButtonElement>,
) {
    buttonBuilder({ +label }, style, isDisabled, isOutline, isActive, classes, title, onClickFun)
}

/**
 * Function to create buttons for modals with icon as content
 *
 * @param icon icon that will be displayed on button
 * @param style color-defining string
 * @param isDisabled flag that might disable button
 * @param isOutline flag that defines either usual button or outlined will be displayed
 * @param isActive flag that defines whether button should be displayed as pressed or not
 * @param classes additional classes for button
 * @param title title for tooltip
 * @param onClickFun button click handler
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
fun ChildrenBuilder.buttonBuilder(
    icon: FontAwesomeIconModule,
    style: String? = "primary",
    isDisabled: Boolean = false,
    isOutline: Boolean = false,
    isActive: Boolean = false,
    classes: String = "",
    title: String? = null,
    onClickFun: MouseEventHandler<HTMLButtonElement>,
) {
    buttonBuilder({ fontAwesomeIcon(icon) }, style, isDisabled, isOutline, isActive, classes, title, onClickFun)
}

/**
 * Simple and light way to display selector. Use it in input-group.
 *
 * @param selectedValue currently selected value
 * @param values list of possible selections
 * @param classes additional classes for [select] tag
 * @param isDisabled flag that might disable selector
 * @param onChangeFun callback invoked on selector change
 */
fun ChildrenBuilder.selectorBuilder(
    selectedValue: String,
    values: List<String>,
    classes: String = "",
    isDisabled: Boolean = false,
    onChangeFun: ChangeEventHandler<HTMLSelectElement>,
) {
    select {
        className = ClassName(classes)
        disabled = isDisabled
        onChange = onChangeFun
        value = selectedValue
        values.forEach { currentOption ->
            option {
                value = currentOption
                +currentOption
            }
        }
    }
}

@Suppress("TOO_MANY_PARAMETERS", "LongParameterList", "LAMBDA_IS_NOT_LAST_PARAMETER")
private fun ChildrenBuilder.buttonBuilder(
    labelBuilder: ChildrenBuilder.() -> Unit,
    style: String? = "primary",
    isDisabled: Boolean = false,
    isOutline: Boolean = false,
    isActive: Boolean = false,
    classes: String = "",
    title: String? = null,
    onClickFun: MouseEventHandler<HTMLButtonElement>,
) {
    button {
        type = ButtonType.button
        val builtClasses = buildString {
            append("btn")
            style?.let {
                append(" btn-")
                if (isOutline) {
                    append("outline-")
                }
                append(it)
                if (isActive) {
                    append(" active")
                }
            }
            append(" $classes")
        }
        className = ClassName(builtClasses)
        disabled = isDisabled
        onClick = onClickFun
        title?.let {
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "top"
            this.title = title
        }
        labelBuilder()
    }
}
