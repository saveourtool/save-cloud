@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.Organization
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import org.w3c.dom.HTMLSelectElement
import org.w3c.fetch.Headers
import react.FC
import react.Props
import react.dom.events.ChangeEvent
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.useState

/**
 * Component with required selection
 */
val selectFormRequired = selectFormRequired()

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

    /**
     * Callback invoked when form is changed
     */
    @Suppress("TYPE_ALIAS")
    var onChangeFun: (form: InputTypes, organization: ChangeEvent<HTMLSelectElement>, isProject: Boolean) -> Unit
}

@Suppress(
    "TOO_MANY_PARAMETERS",
    "TOO_LONG_FUNCTION",
    "LongParameterList",
    "TYPE_ALIAS",
    "LongMethod",
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
                    loadingHandler = ::loadingHandler,
                )
                    .unsafeMap {
                        it.decodeFromJsonString<List<Organization>>()
                    }

        setElements(organizations.map { it.name })
    }()

    div("${props.classes} mt-1") {
        label("form-label") {
            props.form?.let { attrs["htmlFor"] = it.name }
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
                    attrs["className"] = "form-control"
                } else {
                    attrs["className"] = "form-control is-invalid"
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
