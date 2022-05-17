@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import org.w3c.dom.events.Event
import react.RBuilder
import react.dom.attrs
import react.dom.div
import react.dom.input
import react.dom.label
import react.dom.span

import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction

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
    onChangeFun: (Event) -> Unit
) =
        div("$classes mt-1") {
            label("form-label") {
                attrs.set("for", form.name)
                +text
            }

            div("input-group has-validation") {
                span("input-group-text") {
                    attrs["id"] = "${form.name}Span"
                    +"*"
                }

                val type = if (form == InputTypes.PASSWORD) InputType.password else InputType.text
                input(type = type) {
                    attrs {
                        onChangeFunction = onChangeFun
                    }
                    attrs["id"] = form.name
                    attrs["required"] = true
                    if (validInput) {
                        attrs["class"] = "form-control"
                    } else {
                        attrs["class"] = "form-control is-invalid"
                    }
                }

                if (!validInput) {
                    if (isProjectOrOrganizationName) {
                        div("invalid-feedback d-block") {
                            +"Please input a valid ${form.str}. The name can be no longer than 64 characters and can't contain any spaces."
                        }
                    } else {
                        div("invalid-feedback d-block") {
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
internal fun RBuilder.inputTextFormOptional(
    form: InputTypes,
    classes: String,
    text: String,
    onChangeFun: (Event) -> Unit
) =
        div("$classes pl-2 pr-2") {
            label("form-label") {
                attrs.set("for", form.name)
                +text
            }
            input(type = InputType.text) {
                attrs {
                    onChangeFunction = onChangeFun
                }
                attrs["aria-describedby"] = "${form.name}Span"
                attrs["id"] = form.name
                attrs["required"] = false
                attrs["class"] = "form-control"
            }
        }
