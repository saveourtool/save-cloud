/**
 * Optional form FC
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.inputform

import com.saveourtool.save.frontend.externals.fontawesome.faQuestionCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.useTooltipAndPopover
import csstype.ClassName
import org.w3c.dom.HTMLInputElement
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.aria.ariaDescribedBy
import react.dom.events.ChangeEvent
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.sup

/**
 * constant FC to avoid re-creation
 */
val inputTextFormOptionalWrapperConst = inputTextFormOptionalWrapper()

/**
 * Properties for a inputTextFormOptional FC
 */
external interface InputTextFormOptionalProps : Props {
    /**
     * type of a form
     */
    var form: InputTypes

    /**
     * value for the input
     */
    var textValue: String?

    /**
     * HTML bootstrap classes (className)
     */
    var classes: String

    /**
     * label that will be used in a description of an input form
     */
    var name: String?

    /**
     * check (this check will be used to validate an input and mark it red or green)
     */
    var validInput: Boolean?

    /**
     * text of an error in case of invalid input
     */
    var errorText: String?

    /**
     * lambda for a change of input form
     */
    var onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
}

/**
 * Unfortunately we had to wrap temporary this method with FC because of a useEffect statement
 *
 * @param form
 * @param classes
 * @param name
 * @param validInput
 * @param onChangeFun
 * @param errorText
 * @param textValue
 * @param onClickFun
 * @return div with an input form
 */
@Suppress(
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
    "TOO_LONG_FUNCTION",
    "LongMethod"
)
private fun ChildrenBuilder.inputTextFormOptional(
    form: InputTypes,
    textValue: String?,
    classes: String,
    name: String?,
    validInput: Boolean?,
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit,
) {
    div {
        className = ClassName(classes)

        name?.let { name ->
            label {
                className = ClassName("form-label")
                htmlFor = form.name
                +name
            }
        }

        form.popover?.let {
            sup {
                className = ClassName("form-popover")
                fontAwesomeIcon(icon = faQuestionCircle)
                tabIndex = 0
                asDynamic()["popover-placement"] = "left"
                asDynamic()["popover-content"] = it.content
                asDynamic()["popover-title"] = it.title
                asDynamic()["data-trigger"] = "focus"
            }
        }
        input {
            type = InputType.text
            onChange = onChangeFun
            ariaDescribedBy = "${form.name}Span"
            id = form.name
            required = false
            value = textValue
            placeholder = form.placeholder
            form.tooltip?.let {
                title = it
                asDynamic()["tooltip-placement"] = "right"
            }
            className = if (textValue.isNullOrEmpty()) {
                ClassName("form-control")
            } else if (validInput != false) {
                ClassName("form-control is-valid")
            } else {
                ClassName("form-control is-invalid")
            }
        }
        if (validInput == false && !textValue.isNullOrEmpty()) {
            div {
                className = ClassName("invalid-feedback d-block")
                +(form.errorMessage ?: "Please input a valid ${form.str}")
            }
        }
    }
}

/**
 * @return functional component (wrapper for ChildrenBuilder)
 */
fun inputTextFormOptionalWrapper() = FC<InputTextFormOptionalProps> { props ->
    inputTextFormOptional(
        props.form,
        props.textValue,
        props.classes,
        props.name,
        props.validInput,
        props.onChangeFun
    )

    useTooltipAndPopover()
}
