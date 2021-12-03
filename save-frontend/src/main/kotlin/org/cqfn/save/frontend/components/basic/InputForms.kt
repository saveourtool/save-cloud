package org.cqfn.save.frontend.components.basic

import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.events.Event
import react.RBuilder
import react.dom.attrs
import react.dom.div
import react.dom.input
import react.dom.label
import react.dom.span

@Suppress("TOO_LONG_FUNCTION")
internal fun RBuilder.inputTextFormRequired(
    form: InputTypes,
    validInput: Boolean,
    classes: String,
    text: String,
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

            val type  = if (form == InputTypes.PASSWORD) InputType.password else InputType.text
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
                div("invalid-feedback d-block") {
                    +"Please input a valid ${form.str}"
                }
            }
        }
    }

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

/**
 * @property str
 */
internal enum class InputTypes(val str: String) {
    // ==== signIn view
    LOGIN("login"),
    PASSWORD("password"),

    // ==== new project view
    DESCRIPTION("project description"),
    GIT_BRANCH("git branch"),
    GIT_TOKEN("git token"),
    GIT_URL("git url"),
    GIT_USER("git username"),
    OWNER("owner name"),
    PROJECT_NAME("project name"),
    PROJECT_URL("project Url"),
    ;
}
