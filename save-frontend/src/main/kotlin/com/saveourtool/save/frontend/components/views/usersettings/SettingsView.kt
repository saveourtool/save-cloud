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

import react.*
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
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
                className = ClassName("row justify-content-center mt-5")
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
        oldName = if (newName != userInfo.name) userInfo.name else null,
        email = fieldsMap[InputTypes.USER_EMAIL]?.trim(),
        company = fieldsMap[InputTypes.COMPANY]?.trim(),
        location = fieldsMap[InputTypes.LOCATION]?.trim(),
        linkedin = fieldsMap[InputTypes.LINKEDIN]?.trim(),
        gitHub = fieldsMap[InputTypes.GITHUB]?.trim(),
        twitter = fieldsMap[InputTypes.TWITTER]?.trim(),
    )

/*
    useRequest {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }

        val response = post(
            "$apiUrl/users/save",
            headers,
            Json.encodeToString(newUserInfo),
            loadingHandler = ::loadingHandler,
        )
        if (response.isConflict()) {
            val responseText = response.unpackMessage()
            setState {
                conflictErrorMessage = responseText
            }
        } else {
            setState {
                conflictErrorMessage = null
            }
        }
    }
*/
}

fun ChildrenBuilder.inputForm(
    previousValue: String?,
    inputTypes: InputTypes,
    fieldsMap: MutableMap<InputTypes, String>,
    validationToolTips: MutableMap<InputTypes, String>,
    setFieldsMap: StateSetter<MutableMap<InputTypes, String>>,
) {
    div {
        className = ClassName("row")
        div {
            className = ClassName("col-5 mt-2 text-left align-self-center")
            +"${inputTypes.str}:"
        }
        div {
            className = ClassName("col-7 mt-2 input-group pl-0")
            ReactHTML.input {
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
            p {
                +validationToolTips[inputTypes]
            }
        }
    }
}
