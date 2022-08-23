package com.saveourtool.save.frontend.components.inputform

import com.saveourtool.save.frontend.externals.fontawesome.faQuestionCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
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
import react.useEffect

// language=JS
private const val ENABLE_TOOLTIP_AND_POPOVER_JS: String = """
    var jQuery = require("jquery")
    require("popper.js")
    require("bootstrap")
    jQuery('.form-popover').each(function() {
        jQuery(this).popover({
            placement: jQuery(this).attr("popover-placement"),
            title: jQuery(this).attr("popover-title"),
            content: jQuery(this).attr("popover-content"),
            html: true
        }).on('show.bs.popover', function() {
            jQuery(this).tooltip('hide')
        }).on('hide.bs.popover', function() {
            jQuery(this).tooltip('show')
        })
    })
"""


val inputTextFormOptionalWrapperConst = inputTextFormOptionalWrapper()

external interface InputTextFormOptionalProps : Props {
    var form: InputTypes
    var textValue: String?
    var classes: String
    var name: String?
    var validInput: Boolean?
    var errorText: String?
    var onClickFun: () -> Unit
    var onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
}

fun inputTextFormOptionalWrapper() = FC<InputTextFormOptionalProps> { props ->
    inputTextFormOptional(
        props.form,
        props.textValue,
        props.classes,
        props.name,
        props.validInput,
        props.onClickFun,
        props.onChangeFun
    )

    useEffect {
        js(ENABLE_TOOLTIP_AND_POPOVER_JS)
        return@useEffect
    }
}


/**
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
    "TOO_MANY_PARAMETERS", "LongParameterList", "TOO_LONG_FUNCTION", "LongMethod"
)
private fun ChildrenBuilder.inputTextFormOptional(
    form: InputTypes,
    textValue: String?,
    classes: String,
    name: String?,
    validInput: Boolean?,
    onClickFun: () -> Unit,
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit,
) {
    div {
        className = ClassName(classes)
        div {
            className = ClassName("row")
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
        }
        input {
            type = InputType.text
            onClick = { onClickFun() }
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
