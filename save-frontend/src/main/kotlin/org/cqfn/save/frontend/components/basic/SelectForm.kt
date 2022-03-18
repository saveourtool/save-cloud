@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.frontend.components.basic

import org.w3c.dom.events.Event
import react.RBuilder
import react.dom.*

import kotlinx.html.js.onChangeFunction

/**
 * @param form
 * @param validInput
 * @param classes
 * @param text
 * @param onChangeFun
 * @param elements
 * @param initialValue
 * @return div with an input form
 */
@Suppress(
    "TOO_MANY_PARAMETERS",
    "TOO_LONG_FUNCTION",
    "LongParameterList"
)
internal fun RBuilder.selectFormRequired(
    form: InputTypes,
    validInput: Boolean,
    classes: String,
    text: String,
    elements: List<String>,
    initialValue: String?,
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

                select("form-control") {
                    attrs["id"] = form.name
                    attrs["required"] = true
                    if (validInput) {
                        attrs["class"] = "form-control"
                    } else {
                        attrs["class"] = "form-control is-invalid"
                    }

                    val newElements = elements.toMutableList()
                    newElements.add(0, "")
                    newElements.forEach { element ->
                        option {
                            if (element == initialValue) {
                                attrs.selected = true
                            }
                            +element
                        }
                    }

                    attrs.onChangeFunction = onChangeFun
                }

                if (!validInput) {
                    div("invalid-feedback d-block") {
                        +"Please input a valid ${form.str}"
                    }
                }
            }
        }
