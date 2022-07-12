@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic

import csstype.ClassName
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.ChildrenBuilder
import react.RBuilder
import react.dom.*
import react.dom.aria.ariaDescribedBy
import react.dom.events.ChangeEvent
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.span

import kotlinx.html.classes
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.onChange

/**
 * @property str
 */
@Suppress("WRONG_DECLARATIONS_ORDER")
enum class InputTypes(val str: String) {
    // ==== new project view
    DESCRIPTION("project description"),
    GIT_BRANCH("git branch"),
    GIT_TOKEN("git token"),
    GIT_URL("git url"),
    GIT_USER("git username"),

    // ==== signIn view
    LOGIN("login"),
    PASSWORD("password"),
    PROJECT_NAME("project name"),
    PROJECT_URL("project Url"),

    // ==== create organization view
    ORGANIZATION_NAME("organization name"),

    // ==== user setting view
    USER_EMAIL("user email"),
    COMPANY("company"),
    LOCATION("location"),
    GIT_HUB("git hub"),
    LINKEDIN("linkedin"),
    TWITTER("twitter"),
    ;
}

/**
 * @param form
 * @param validInput
 * @param classes
 * @param text
 * @param isProjectOrOrganizationName
 * @param onChangeFun
 * @return div with an input form
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
)
internal fun RBuilder.inputTextFormRequired(
    form: InputTypes,
    validInput: Boolean,
    classes: String,
    text: String,
    isProjectOrOrganizationName: Boolean = false,
    onChangeFun: (dynamic) -> Unit
) =
        div("$classes mt-1") {
            label("form-label") {
                attrs.htmlFor = form.name
                +text
            }

            div("input-group has-validation") {
                span("input-group-text") {
                    attrs["id"] = "${form.name}Span"
                    +"*"
                }

                val inputType = if (form == InputTypes.PASSWORD) {
                    kotlinx.html.InputType.password
                } else {
                    kotlinx.html.InputType.text
                }
                input {
                    attrs.type = inputType
                    attrs.onChangeFunction = onChangeFun as (Event) -> Unit
                    attrs.id = form.name
                    attrs.required = true
                    if (validInput) {
                        attrs.classes = setOf("form-control")
                    } else {
                        attrs.classes = setOf("form-control", "is-invalid")
                    }
                }

                if (!validInput) {
                    if (isProjectOrOrganizationName) {
                        div {
                            attrs["classes"] = setOf("invalid-feedback", "d-block")
                            +"Please input a valid ${form.str}. The name can be no longer than 64 characters and can't contain any spaces."
                        }
                    } else {
                        div {
                            attrs["classes"] = setOf("invalid-feedback", "d-block")
                            +"Please input a valid ${form.str}"
                        }
                    }
                }
            }
        }

/**
 * @param form
 * @param validInput
 * @param classes
 * @param text
 * @param isProjectOrOrganizationName
 * @param onChangeFun
 * @return div with an input form
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
)
internal fun ChildrenBuilder.inputTextFormRequired(
    form: InputTypes,
    validInput: Boolean,
    classes: String,
    text: String,
    isProjectOrOrganizationName: Boolean = false,
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
) =
        div {
            className = ClassName("$classes mt-1")
            label {
                className = ClassName("form-label")
                htmlFor = form.name
                +text
            }

            div {
                className = ClassName("input-group has-validation")
                span {
                    className = ClassName("input-group-text")
                    id = "${form.name}Span"
                    +"*"
                }

                val inputType = if (form == InputTypes.PASSWORD) InputType.password else InputType.text
                input {
                    type = inputType
                    onChange = onChangeFun
                    id = form.name
                    required = true
                    if (validInput) {
                        className = ClassName("form-control")
                    } else {
                        className = ClassName("form-control is-invalid")
                    }
                }

                if (!validInput) {
                    if (isProjectOrOrganizationName) {
                        div {
                            className = ClassName("invalid-feedback d-block")
                            +"Please input a valid ${form.str}. The name can be no longer than 64 characters and can't contain any spaces."
                        }
                    } else {
                        div {
                            className = ClassName("invalid-feedback d-block")
                            +"Please input a valid ${form.str}"
                        }
                    }
                }
            }
        }

/**
 * @param form
 * @param classes
 * @param text
 * @param onChangeFun
 * @return div with an input form
 */
internal fun ChildrenBuilder.inputTextFormOptional(
    form: InputTypes,
    classes: String,
    text: String,
    onChangeFun: (ChangeEvent<HTMLInputElement>) -> Unit
) =
        div {
            className = ClassName("$classes pl-2 pr-2")
            label {
                className = ClassName("form-label")
                htmlFor = form.name
                +text
            }
            input {
                type = InputType.text
                onChange = onChangeFun
                ariaDescribedBy = "${form.name}Span"
                id = form.name
                required = false
                className = ClassName("form-control")
            }
        }

/**
 * @param form
 * @param classes
 * @param text
 * @param onChangeFun
 * @return div with an input form
 */
internal fun RBuilder.inputTextFormOptional(
    form: InputTypes,
    classes: String,
    text: String,
    onChangeFun: (dynamic) -> Unit
) = div("$classes pl-2 pr-2") {
    label("form-label") {
        attrs.htmlFor = form.name
        +text
    }
    input(type = kotlinx.html.InputType.text, classes = "form-control") {
        attrs.onChangeFunction = onChangeFun as (Event) -> Unit
        attrs["aria-describedby"] = "${form.name}Span"
        attrs.id = form.name
        attrs.required = false
    }
}
