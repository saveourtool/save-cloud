@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.entities.Organization
import org.cqfn.save.frontend.utils.*

import org.w3c.dom.events.Event
import org.w3c.fetch.Headers
import react.Props
import react.dom.*
import react.fc
import react.useState

import kotlinx.html.js.onChangeFunction

/**
 * SelectFormRequired component props
 */
external interface SelectFormRequiredProps : Props {
    /**
     * Type of 'select'
     */
    var form: InputTypes?

    /**
     * Flag to valid select
     */
    var validInput: Boolean?

    /**
     * classes of 'select'
     */
    var classes: String?

    /**
     * select name
     */
    var text: String?
}

/**
 * @param onChangeFun
 * @return div with an input form
 */
@Suppress(
    "TOO_MANY_PARAMETERS",
    "TOO_LONG_FUNCTION",
    "LongParameterList",
    "TYPE_ALIAS",
)
fun selectFormRequired(
    onChangeFun: (form: InputTypes, organization: Event, isProject: Boolean) -> Unit
) = fc<SelectFormRequiredProps> { props ->

    val (elements, setElements) = useState(listOf<String>())

    useRequest(arrayOf(), isDeferred = false) {
        val organizations =
                get(
                    url = "$apiUrl/organization/get/list",
                    headers = Headers().also {
                        it.set("Accept", "application/json")
                    },
                )
                    .unsafeMap {
                        it.decodeFromJsonString<List<Organization>>()
                    }

        setElements(organizations.map { it.name })
    }()

    div("${props.classes} mt-1") {
        label("form-label") {
            props.form?.let { attrs.set("for", it.name) }
            +"${props.text}"
        }

        div("input-group has-validation") {
            span("input-group-text") {
                attrs["id"] = "${props.form?.name}Span"
                +"*"
            }

            select("form-control") {
                attrs["id"] = "${props.form?.name}"
                attrs["required"] = true
                if (props.validInput == true) {
                    attrs["class"] = "form-control"
                } else {
                    attrs["class"] = "form-control is-invalid"
                }

                val newElements = elements.toMutableList()
                newElements.add(0, "")
                newElements.forEach { element ->
                    option {
                        +element
                    }
                }

                attrs.onChangeFunction = {
                    onChangeFun(props.form!!, it, true)
                }
            }

            if (elements.isEmpty()) {
                div("invalid-feedback d-block") {
                    +"You don't have access to any organizations"
                }
            } else if (props.validInput == false) {
                div("invalid-feedback d-block") {
                    +"Please input a valid ${props.form?.str}"
                }
            }
        }
    }
}
