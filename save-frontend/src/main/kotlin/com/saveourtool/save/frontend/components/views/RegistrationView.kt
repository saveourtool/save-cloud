/**
 * A view for registration
 */

@file:Suppress("FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.inputTextFormRequired
import com.saveourtool.save.frontend.components.modal.MAX_Z_INDEX
import com.saveourtool.save.frontend.http.postImageUpload
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.classLoadingHandler
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.v1
import com.saveourtool.save.validation.isValidName

import js.core.asList
import js.core.jso
import react.*
import react.dom.events.ChangeEvent
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.span
import react.router.Navigate
import web.cssom.ClassName
import web.cssom.ZIndex
import web.cssom.rem
import web.html.ButtonType
import web.html.HTMLInputElement
import web.html.InputType

import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `Props` retrieved from router
 */
external interface RegistrationProps : PropsWithChildren {
    /**
     * Currently logged in user or null
     */
    var userInfo: UserInfo?
}

/**
 * [State] of registration view component
 */
external interface RegistrationViewState : State {
    /**
     * Conflict error message
     */
    var conflictErrorMessage: String?

    /**
     * Validation of input fields
     */
    var isValidUserName: Boolean?

    /**
     * Map for input fields
     */
    var fieldsMap: MutableMap<InputTypes, String>
}

/**
 * A Component for registration view
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class RegistrationView : AbstractView<RegistrationProps, RegistrationViewState>() {
    init {
        state.isValidUserName = true
        state.fieldsMap = mutableMapOf()
    }

    override fun componentDidMount() {
        super.componentDidMount()
        setState {
            props.userInfo?.name?.let { fieldsMap[InputTypes.USER_NAME] = it }
        }
    }

    override fun componentDidUpdate(prevProps: RegistrationProps, prevState: RegistrationViewState, snapshot: Any) {
        if (props.userInfo != prevProps.userInfo) {
            setState {
                props.userInfo?.name?.let { fieldsMap[InputTypes.USER_NAME] = it }
            }
        }
    }
    private fun changeFields(
        fieldName: InputTypes,
        target: ChangeEvent<HTMLInputElement>,
    ) {
        val tg = target.target
        setState {
            fieldsMap[fieldName] = tg.value
        }
    }

    private fun saveUser(inputUpdated: String) {
        val newUserInfo = props.userInfo?.copy(
            name = inputUpdated,
            oldName = props.userInfo!!.name,
            isActive = true,
        )

        scope.launch {
            val response = post(
                "$apiUrl/users/save",
                jsonHeaders,
                Json.encodeToString(newUserInfo),
                loadingHandler = ::classLoadingHandler,
                responseHandler = ::classComponentResponseHandlerWithValidation,
            )
            if (response.ok) {
                window.location.href = "#"
                window.location.reload()
            } else if (response.isConflict()) {
                val responseText = response.unpackMessage()
                setState {
                    conflictErrorMessage = responseText
                }
            }
        }
    }

    @Suppress(
        "EMPTY_BLOCK_STRUCTURE_ERROR",
    )
    override fun ChildrenBuilder.render() {
        particles()

        if (props.userInfo?.isActive != false) {
            Navigate {
                to = "/"
                replace = false
            }
        }

        main {
            className = ClassName("main-content mt-0 ps")
            div {
                className = ClassName("page-header align-items-start min-vh-100")
                span {
                    className = ClassName("mask bg-gradient-dark opacity-6")
                }
                div {
                    className = ClassName("row justify-content-center")
                    div {
                        className = ClassName("col-sm-4")
                        div {
                            className = ClassName("container card o-hidden border-0 shadow-lg my-2 card-body p-0")
                            style = jso {
                                zIndex = (MAX_Z_INDEX - 1).unsafeCast<ZIndex>()
                            }
                            div {
                                className = ClassName("p-5 text-center")
                                renderTitle()
                                renderAvatar()
                                renderInputForm()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ChildrenBuilder.renderTitle() {
        h1 {
            className = ClassName("h4 text-gray-900 mb-4")
            +"Set your user name and avatar"
        }
    }

    @Suppress(
        "MAGIC_NUMBER",
    )
    private fun ChildrenBuilder.renderAvatar() {
        label {
            className = ClassName("btn")
            title = "Change the user's avatar"
            input {
                type = InputType.file
                hidden = true
                onChange = { event ->
                    scope.launch {
                        event.target.files!!.asList().single().let { file ->
                            postImageUpload(file, props.userInfo?.name!!, AvatarType.USER, ::classLoadingHandler)
                        }
                    }
                }
            }
            img {
                className =
                        ClassName("avatar avatar-user width-full border color-bg-default rounded-circle")
                src = props.userInfo?.avatar?.let {
                    "/api/$v1/avatar$it"
                }
                    ?: AVATAR_PROFILE_PLACEHOLDER
                style = jso {
                    height = 16.rem
                    width = 16.rem
                }
            }
        }
    }

    @Suppress(
        "PARAMETER_NAME_IN_OUTER_LAMBDA",
    )
    private fun ChildrenBuilder.renderInputForm() {
        // google does not provide us login, only e-mail, so need to trim everything after "@gmail"
        val rawInput = state.fieldsMap[InputTypes.USER_NAME]?.trim().orEmpty()
        val atIndex = rawInput.indexOf('@')
        val inputUpdated = if (atIndex >= 0) rawInput.substring(0, atIndex) else rawInput
        form {
            div {
                inputTextFormRequired {
                    form = InputTypes.USER_NAME
                    textValue = inputUpdated
                    validInput = inputUpdated.isEmpty() || inputUpdated.isValidName()
                    classes = ""
                    name = "User name"
                    conflictMessage = state.conflictErrorMessage
                    onChangeFun = {
                        changeFields(InputTypes.USER_NAME, it)
                        setState {
                            conflictErrorMessage = null
                        }
                    }
                }
            }
            button {
                type = ButtonType.button
                className = ClassName("btn btn-info mt-4 mr-3")
                +"Registration"
                onClick = {
                    saveUser(inputUpdated)
                }
            }
            state.conflictErrorMessage?.let {
                div {
                    className = ClassName("invalid-feedback d-block")
                    +it
                }
            }
        }
    }
}
