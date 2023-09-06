/**
 * A view with settings user
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.modal.modal
import com.saveourtool.save.frontend.components.views.index.*
import com.saveourtool.save.frontend.components.views.usersettings.right.SettingsInputFields
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import kotlinx.browser.window
import react.*
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.main
import react.router.useNavigate
import web.cssom.*
import web.html.ButtonType
import web.html.InputType

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val cardHeight: CSSProperties = jso {
    height = 53.rem
}

val userSettingsView: FC<SettingsProps> = FC { props ->
    val useNavigate = useNavigate()

    modal { modalProps ->
        modalProps.isOpen = props.userInfo == null
        modalProps.contentLabel = "Unauthenticated"
        div {
            className = ClassName("row align-items-center justify-content-center")
            h2 {
                className = ClassName("h6 text-gray-800")
                +"You are not logged in"
            }
        }
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mt-4")
            button {
                className = ClassName("btn btn-outline-primary")
                type = ButtonType.button
                onClick = {
                    useNavigate(to = "/")
                    window.location.reload()
                }
                +"Proceed to login page"
            }
        }
    }

    useBackground(Style.SAVE_LIGHT)
    main {
        className = ClassName("main-content")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("row justify-content-center mt-3")
                div {
                    className = ClassName("col-2")
                    leftSettingsColumn { this.userInfo = props.userInfo }
                }
                div {
                    className = ClassName("col-7")
                    if (props.userInfo?.name?.isNotEmpty() == true) {
                        rightSettingsColumn {
                            this.userInfo = props.userInfo
                            this.type = props.type
                            this.userInfoSetter = props.userInfoSetter
                        }
                    }
                }
            }
        }
    }
}

typealias FieldsStateSetter = StateSetter<SettingsInputFields>

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface SettingsProps : PropsWithChildren {
    /**
     * Currently logged-in user or null
     */
    var userInfo: UserInfo?

    /**
     * just a flag for a factory
     */
    var type: FrontendRoutes

    /**
     * After updating user information we will update userSettings without re-rendering the page
     * PLEASE NOTE: THIS PROPERTY AFFECTS RENDERING OF WHOLE APP.KT
     * IF YOU HAVE SOME PROBLEMS WITH IT, CHECK THAT YOU HAVE PROPAGATED IT PROPERLY:
     * { this.userInfoSetter = (!) PROPS (!) .userInfoSetter }
     */
    var userInfoSetter: StateSetter<UserInfo?>
}

/**
 * Drawing an input for profile settings (one-liner)
 *
 * @param previousValue
 * @param inputType
 * @param setFields
 * @param placeholderText
 * @param settingsInputFields
 * @param colRatio sizes of columns: <TITLE> =====INPUT FIELD=====
 * @param validationFunction
 */
@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
fun ChildrenBuilder.inputForm(
    previousValue: String?,
    inputType: InputTypes,
    settingsInputFields: SettingsInputFields,
    setFields: FieldsStateSetter,
    placeholderText: String = "",
    colRatio: Pair<String, String> = "col-4" to "col-8",
    validationFunction: String.() -> String,
) {
    div {
        className = ClassName("row justify-content-center")
        div {
            className = ClassName("${colRatio.first} mt-2 text-left align-self-center")
            +"${inputType.str}:"
        }
        div {
            className = ClassName("${colRatio.second} mt-2 input-group pl-0")
            input {
                placeholder = placeholderText
                type = InputType.text
                className = ClassName("form-control shadow")
                previousValue.let {
                    defaultValue = it
                }
                onChange = {
                    val textInTheInput = it.target.value
                    val settingsInputFieldsNew =
                            settingsInputFields.updateValue(inputType, it.target.value, textInTheInput.validationFunction())
                    setFields(settingsInputFieldsNew)
                }
            }
            val validationText = settingsInputFields.getValueByType(inputType).validation
            if (validationText.isNotBlank()) {
                div {
                    className = ClassName("invalid-feedback d-block")
                    +validationText
                }
            }
        }
    }
}

/**
 * (!) HOOK WITH REQUEST
 *
 * @param props
 * @param settingsInputFields
 * @param setFieldsValidation
 * @return callback to a post request that will be executed
 */
fun useSaveUser(
    props: SettingsProps,
    settingsInputFields: SettingsInputFields,
    setFieldsValidation: FieldsStateSetter,
) = useDeferredRequest {
    // this new user info will be sent to backend and also will be set in setter,
    // so frontend will recalculate it on the fly at least for SettingsView (need to extend it later)
    val newUserInfo = settingsInputFields.toUserInfo(props.userInfo!!)

    val response = post(
        "$apiUrl/users/save",
        jsonHeaders,
        Json.encodeToString(newUserInfo),
        loadingHandler = ::loadingHandler,
        responseHandler = ::noopResponseHandler
    )

    if (response.isConflict()) {
        val responseText = response.unpackMessage()
        val newSettingsInputFields = settingsInputFields.updateValue(InputTypes.LOGIN, null, responseText)
        setFieldsValidation(newSettingsInputFields)
    } else {
        val newSettingsInputFields = settingsInputFields.updateValue(InputTypes.LOGIN, null, "")
        setFieldsValidation(newSettingsInputFields)
        props.userInfoSetter(newUserInfo)
    }
}
