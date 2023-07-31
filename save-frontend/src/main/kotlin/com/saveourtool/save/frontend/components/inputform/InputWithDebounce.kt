@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.inputform

import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.DEFAULT_DEBOUNCE_PERIOD
import com.saveourtool.save.v1

import js.core.jso
import org.w3c.fetch.Response
import react.*
import react.dom.html.AutoComplete
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.option
import web.cssom.*
import web.html.InputType
import web.timers.setTimeout
import kotlin.time.Duration.Companion.milliseconds

private const val DROPDOWN_ID = "option-dropdown"

/**
 * Component that encapsulates debounced prefix autocompletion over [UserInfo.name]
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val inputWithDebounceForUserInfo = inputWithDebounce(
    asOption = { UserInfo(name = this) },
    asString = { name },
    decodeListFromJsonString = { decodeFromJsonString() },
)

/**
 * Component that encapsulates debounced prefix autocompletion over [String]
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val inputWithDebounceForString = inputWithDebounce(
    asOption = { this },
    asString = { this },
    decodeListFromJsonString = { decodeFromJsonString() },
)

/**
 * [Props] for [inputWithDebounce] component
 */
external interface InputWithDebounceProps<T> : PropsWithChildren {
    /**
     * Callback to get url for options fetch
     */
    var getUrlForOptionsFetch: (prefix: String) -> String

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
    var renderOption: (ChildrenBuilder, option: T) -> Unit

    /**
     * Debounce period, equals to [DEFAULT_DEBOUNCE_PERIOD] by default
     */
    var debouncePeriod: Int?

    /**
     * Placeholder for form
     */
    var placeholder: String

    /**
     * Callback invoked on option click
     */
    var onOptionClick: (option: T) -> Unit

    /**
     * Maximum amount of options to display
     */
    var maxOptions: Int?
}

/**
 * @param childrenBuilder [ChildrenBuilder] instance
 * @param userInfo user's [UserInfo]
 */
internal fun renderUserWithAvatar(childrenBuilder: ChildrenBuilder, userInfo: UserInfo) {
    with(childrenBuilder) {
        div {
            className = ClassName("row col d-flex align-items-center")
            div {
                className = ClassName("col-1")
                img {
                    className = ClassName("avatar avatar-user border color-bg-default rounded-circle pl-0")
                    src = userInfo.avatar?.let { "/api/$v1/avatar$it" } ?: "/img/undraw_profile.svg"
                    style = jso {
                        width = "2rem".unsafeCast<Width>()
                        height = "2rem".unsafeCast<Height>()
                    }
                }
            }
            h4 {
                className = ClassName("col-auto mb-0")
                +userInfo.name
            }
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun <T> inputWithDebounce(
    asOption: String.() -> T,
    asString: T.() -> String,
    decodeListFromJsonString: suspend Response.() -> List<T>,
) = FC<InputWithDebounceProps<T>> { props ->
    val (options, setOptions) = useState<List<T>>(emptyList())
    val getOptions = useDebouncedDeferredRequest(props.debouncePeriod ?: DEFAULT_DEBOUNCE_PERIOD) {
        if (props.selectedOption.asString().isNotBlank()) {
            val optionsFromBackend: List<T> = get(
                url = props.getUrlForOptionsFetch(props.selectedOption.asString()),
                headers = jsonHeaders,
                loadingHandler = ::noopLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
                .unsafeMap { it.decodeListFromJsonString() }
            setOptions(optionsFromBackend)
        } else {
            setOptions(emptyList())
        }
    }

    useEffect(props.selectedOption) { getOptions() }

    div {
        className = ClassName("")
        id = "input-with-$DROPDOWN_ID"
        style = jso {
            width = "100%".unsafeCast<Width>()
            position = "relative".unsafeCast<Position>()
            zIndex = "2".unsafeCast<ZIndex>()
        }
        onBlur = { setTimeout(ON_BLUR_TIMEOUT_MILLIS.milliseconds) { setOptions(emptyList()) } }
        div {
            className = ClassName("input-group")
            input {
                className = ClassName("form-control")
                id = "input-with-autocompletion"
                type = InputType.text
                placeholder = props.placeholder
                autoComplete = "off".unsafeCast<AutoComplete>()
                value = props.selectedOption.asString()
                onChange = { props.setSelectedOption(it.target.value.asOption()) }
            }
            props.children?.let { +it }
        }
        div {
            className = ClassName("list-group")
            id = DROPDOWN_ID
            style = jso {
                position = "absolute".unsafeCast<Position>()
                top = "100%".unsafeCast<Top>()
                width = "100%".unsafeCast<Width>()
                zIndex = "3".unsafeCast<ZIndex>()
            }
            options.let { optionList ->
                props.maxOptions?.let {
                    optionList.take(it)
                } ?: options
            }.forEachIndexed { idx, option ->
                div {
                    className = ClassName("list-group-item list-group-item-action")
                    id = "$DROPDOWN_ID-$idx"
                    onClick = { props.onOptionClick(option) }
                    props.renderOption(this, option)
                }
            }
        }
    }
}
