@file:Suppress("FILE_NAME_MATCH_CLASS", "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.span
import react.useState

/**
 * SelectFormRequired component props
 */
external interface SelectFormRequiredProps<D : Any> : Props {
    /**
     * Type of 'select'
     */
    var formType: InputTypes

    /**
     * Flag to valid select
     */
    var validInput: Boolean?

    /**
     * classes of 'select'
     */
    var classes: String

    /**
     * select name
     */
    var formName: String

    /**
     * lambda invoked once to fetch data for selection
     */
    var getData: suspend WithRequestStatusContext.() -> List<D>

    /**
     * Currently chosen field
     */
    var selectedValue: String

    /**
     * Method to get string that should be shown
     */
    var dataToString: (D) -> String

    /**
     * Message shown on invalid input
     */
    var errorMessage: String?

    /**
     * Message shown on no options fetched
     */
    var notFoundErrorMessage: String?

    /**
     * Callback invoked when form is changed
     */
    @Suppress("TYPE_ALIAS")
    var onChangeFun: (D?) -> Unit
}

/**
 * @return [FC] of required selection input form
 */
@Suppress(
    "TOO_MANY_PARAMETERS",
    "TOO_LONG_FUNCTION",
    "LongParameterList",
    "TYPE_ALIAS",
    "LongMethod",
)
fun <D : Any> selectFormRequired() = FC<SelectFormRequiredProps<D>> { props ->
    val (elements, setElements) = useState(listOf<D>())

    useRequest(arrayOf(), isDeferred = false) {
        setElements((props.getData)())
    }()

    div {
        className = ClassName("${props.classes} mt-1")
        label {
            className = ClassName("form-label")
            props.formType.let {
                htmlFor = it.name
            }
            +props.formName
            span {
                className = ClassName("text-danger")
                id = "${props.formType.name}Span"
                +"*"
            }
        }

        div {
            className = ClassName("input-group has-validation")
            select {
                className = ClassName("form-control")
                id = props.formType.name
                required = true
                option {
                    disabled = true
                    +""
                }
                value = props.selectedValue
                elements.forEach { element ->
                    option {
                        +props.dataToString(element)
                    }
                }
                className = when {
                    value == "" || value == null -> ClassName("form-control")
                    props.validInput == true -> ClassName("form-control is-valid")
                    props.validInput == false -> ClassName("form-control is-invalid")
                    else -> ClassName("form-control")
                }
                onChange = { event ->
                    elements.find {
                        props.dataToString(it) == event.target.value
                    }?.let {
                        props.onChangeFun(it)
                    }
                }
            }

            if (elements.isEmpty()) {
                props.notFoundErrorMessage?.let {
                    +it
                }
            } else if (props.validInput == false) {
                div {
                    className = ClassName("invalid-feedback d-block")
                    +(props.errorMessage ?: "Please input a valid ${props.formType.str}")
                }
            }
        }
    }
}
