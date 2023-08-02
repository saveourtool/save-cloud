/**
 * A view with settings user
 */

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.views.index.*
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.Headers

import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.p
import web.cssom.*
import web.html.InputType


val cardHeight: CSSProperties = jso {
    height = 50.rem
}

val userSettingsView = FC<SettingsProps> { props ->
    useBackground(Style.SAVE_LIGHT)
    main {
        className = ClassName("main-content")
        div {
            className = ClassName("page-header align-items-start min-vh-100")
            div {
                className = ClassName("row justify-content-center mt-3")
                div {
                    className = ClassName("col-2")
                    leftColumn { this.userInfo = props.userInfo }
                }
                div {
                    className = ClassName("col-7")
                    props.userInfo?.let {
                        rightColumn {
                            this.userInfo = props.userInfo
                            this.type = props.type
                        }
                    } ?: main {
                        +"404"
                    }
                }
            }
        }
    }
}

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface SettingsProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?

    /**
     * just a flag for a factory
     */
    var type: FrontendRoutes
}

@Suppress("MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
fun createNewUser(fieldsMap: MutableMap<InputTypes, String>, userInfo: UserInfo): UserInfo {
    val newName = fieldsMap[InputTypes.USER_NAME]?.trim()
    return userInfo.copy(
        name = newName ?: userInfo.name,
        // `oldName` is not saved into database, basically it's just a flag for
        // backend to understand that name was or wasn't changed on the frontend
        // need to pass `null` to backend if the field
        oldName = newName?.let { userInfo.name },
        email = fieldsMap[InputTypes.USER_EMAIL]?.trim() ?: userInfo.email,
        company = fieldsMap[InputTypes.COMPANY]?.trim() ?: userInfo.company,
        location = fieldsMap[InputTypes.LOCATION]?.trim() ?: userInfo.location,
        linkedin = fieldsMap[InputTypes.LINKEDIN]?.trim() ?: userInfo.linkedin,
        gitHub = fieldsMap[InputTypes.GITHUB]?.trim() ?: userInfo.gitHub,
        twitter = fieldsMap[InputTypes.TWITTER]?.trim() ?: userInfo.twitter,
        website = fieldsMap[InputTypes.WEBSITE]?.trim() ?: userInfo.twitter,
        realName = fieldsMap[InputTypes.REAL_NAME]?.trim() ?: userInfo.realName,
        freeText = fieldsMap[InputTypes.FREE_TEXT]?.trim() ?: userInfo.freeText,
    )
}

fun ChildrenBuilder.inputForm(
    previousValue: String?,
    inputTypes: InputTypes,
    fieldsMap: MutableMap<InputTypes, String>,
    validationToolTips: MutableMap<InputTypes, String?>,
    setFieldsMap: StateSetter<MutableMap<InputTypes, String>>,
    placeholderText: String = "",
    ) {
    div {
        className = ClassName("row")
        div {
            className = ClassName("col-4 mt-2 text-left align-self-center")
            +"${inputTypes.str}:"
        }
        div {
            className = ClassName("col-8 mt-2 input-group pl-0")
           input {
                placeholder = placeholderText
                type = InputType.text
                className = ClassName("form-control")
                previousValue.let {
                    defaultValue = it
                }
                onChange = {
                    fieldsMap[inputTypes] = it.target.value
                    setFieldsMap(fieldsMap)
                }
            }
            validationToolTips[inputTypes]?.let {
                div {
                    className = ClassName("invalid-feedback d-block")
                    +it
                }
            }
        }
    }
}

fun saveUser(
    fieldsMap: MutableMap<InputTypes, String>,
    props: SettingsProps,
    validationToolTips: MutableMap<InputTypes, String?>,
    setValidationToolTips: StateSetter<MutableMap<InputTypes, String?>>
) = useDeferredRequest {
    val response = post(
        "$apiUrl/users/save",
        Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        },
        Json.encodeToString(createNewUser(fieldsMap, props.userInfo!!)),
        loadingHandler = ::loadingHandler,
        responseHandler = ::noopResponseHandler
    )

    if (response.isConflict()) {
        val responseText = response.unpackMessage()
        validationToolTips[InputTypes.USER_NAME] = responseText
        setValidationToolTips(validationToolTips)
    } else {
        validationToolTips[InputTypes.USER_NAME] = null
        setValidationToolTips(validationToolTips)
        window.location.reload()
    }
}
