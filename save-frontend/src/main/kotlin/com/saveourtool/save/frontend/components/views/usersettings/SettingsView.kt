/**
 * A view with settings user
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.views.usersettings

import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.views.index.*
import com.saveourtool.save.frontend.components.views.usersettings.right.FieldsStateSetter
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.validation.FrontendRoutes

import js.core.jso
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.main
import web.cssom.*
import web.html.InputType

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val cardHeight: CSSProperties = jso {
    height = 50.rem
}

val userSettingsView: FC<SettingsProps> = FC { props ->
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
                        // FixMe: some light 404
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

/**
 * Drawing an input for profile settings (one-liner)
 *
 * @param previousValue
 * @param inputType
 * @param fieldsMap
 * @param fieldsValidationMap
 * @param setFieldsMap
 * @param placeholderText
 */
@Suppress("TOO_MANY_PARAMETERS")
fun ChildrenBuilder.inputForm(
    previousValue: String?,
    inputType: InputTypes,
    fieldsMap: MutableMap<InputTypes, String?>,
    fieldsValidationMap: MutableMap<InputTypes, String?>,
    setFieldsMap: FieldsStateSetter,
    placeholderText: String = "",
) {
    div {
        className = ClassName("row")
        div {
            className = ClassName("col-4 mt-2 text-left align-self-center")
            +"${inputType.str}:"
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
                    fieldsMap[inputType] = it.target.value
                    setFieldsMap(fieldsMap)
                }
            }
            fieldsValidationMap[inputType]?.let {
                div {
                    className = ClassName("invalid-feedback d-block")
                    +it
                }
            }
        }
    }
}

/**
 * @param fieldsMap
 * @param props
 * @param fieldsValidationMap
 * @param setFieldsValidationMap
 * @return callback to a post request that will be executed
 */
fun saveUser(
    fieldsMap: MutableMap<InputTypes, String?>,
    props: SettingsProps,
    fieldsValidationMap: MutableMap<InputTypes, String?>,
    setFieldsValidationMap: FieldsStateSetter,
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
        fieldsValidationMap[InputTypes.USER_NAME] = responseText
        setFieldsValidationMap(fieldsValidationMap)
    } else {
        fieldsValidationMap[InputTypes.USER_NAME] = null
        setFieldsValidationMap(fieldsValidationMap)
        window.location.reload()
    }
}

private fun createNewUser(fieldsMap: MutableMap<InputTypes, String?>, userInfo: UserInfo): UserInfo {
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
