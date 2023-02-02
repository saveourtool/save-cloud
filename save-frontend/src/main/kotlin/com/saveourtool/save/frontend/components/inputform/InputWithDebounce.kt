@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.inputform

import com.saveourtool.save.frontend.externals.lodash.debounce
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.DEFAULT_DEBOUNCE_PERIOD

import csstype.ClassName
import csstype.None
import js.core.jso
import org.w3c.fetch.Response
import react.*
import react.dom.html.ReactHTML.datalist
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import web.html.InputType

/**
 * Component that encapsulates debounced prefix autocompletion over [UserInfo]'s name field
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val inputWithDebounceForUserInfo = inputWithDebounce<UserInfo>()

/**
 * Component that encapsulates debounced prefix autocompletion over String
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val inputWithDebounceForString = inputWithDebounce<String>()

/**
 * [Props] for [inputWithDebounce] component
 */
external interface InputWithDebounceProps<T> : Props {
    /**
     * Callback to get url for options fetch
     */
    var getUrlForOptions: (prefix: String) -> String

    /**
     * Callback to get string from option
     */
    var getString: (option: T) -> String

    /**
     * Callback to get option from string
     */
    var getOptionFromString: (optionAsString: String) -> T

    /**
     * Currently selected option
     */
    var selectedOption: T

    /**
     * Callback to set selected option
     */
    var setSelectedOption: (option: T) -> Unit

    /**
     * Callback to create [option] tag from [T]
     */
    @Suppress("VARIABLE_NAME_INCORRECT_FORMAT", "TYPE_ALIAS")
    var getHTMLDataListElementFromOption: (ChildrenBuilder, option: T) -> Unit

    /**
     * Debounce period, equals to [DEFAULT_DEBOUNCE_PERIOD] by default
     */
    var debouncePeriod: Int?

    /**
     * Callback to decode [Response] into list of options
     */
    var decodeListFromJsonString: suspend (Response) -> List<T>

    /**
     * Placeholder for form
     */
    var placeholder: String
}

private fun <T> inputWithDebounce() = FC<InputWithDebounceProps<T>> { props ->
    val (options, setOptions) = useState<List<T>>(emptyList())
    val getOptions = debounce(
        useDeferredRequest {
            if (props.getString(props.selectedOption).isNotBlank()) {
                val optionsFromBackend: List<T> = get(
                    url = props.getUrlForOptions(props.getString(props.selectedOption)),
                    headers = jsonHeaders,
                    loadingHandler = ::noopLoadingHandler,
                    responseHandler = ::noopResponseHandler,
                )
                    .unsafeMap {
                        props.decodeListFromJsonString(it)
                    }
                setOptions(optionsFromBackend)
            } else {
                setOptions(emptyList())
            }
        },
        props.debouncePeriod ?: DEFAULT_DEBOUNCE_PERIOD,
    )

    useEffect(props.selectedOption) {
        getOptions()
    }

    input {
        type = InputType.text
        className = ClassName("form-control")
        id = "input-with-autocompletion"
        list = "completions-for-input"
        placeholder = props.placeholder
        value = props.getString(props.selectedOption)
        onChange = {
            props.setSelectedOption(props.getOptionFromString(it.target.value))
        }
    }
    datalist {
        id = "completions-for-input"
        style = jso {
            appearance = None.none
        }
        for (option in options) {
            props.getHTMLDataListElementFromOption(this, option)
        }
    }
}
