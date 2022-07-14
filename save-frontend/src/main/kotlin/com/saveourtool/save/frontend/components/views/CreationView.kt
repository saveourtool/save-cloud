
/**
 * A view with project creation details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.entities.*
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.InputTypes
import com.saveourtool.save.frontend.components.basic.inputTextFormOptional
import com.saveourtool.save.frontend.components.basic.inputTextFormRequired
import com.saveourtool.save.frontend.components.basic.selectFormRequired
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.fontawesome.faQuestionCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*

import csstype.ClassName
import org.w3c.dom.*
import org.w3c.fetch.Headers
import react.*
import react.dom.*
import react.dom.aria.AriaRole
import react.dom.aria.ariaDescribedBy
import react.dom.events.ChangeEvent
import react.dom.html.ButtonType
import react.dom.html.InputType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.main
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.textarea

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [State] of project creation view component
 */
external interface ProjectSaveViewState : State {
    /**
     * Flag to handle error
     */
    var isErrorWithProjectSave: Boolean?

    /**
     * Error message
     */
    var errorMessage: String

    /**
     * Validation of input fields
     */
    var isValidOrganization: Boolean?

    /**
     * Validation of input fields
     */
    var isValidProjectName: Boolean?

    /**
     * Validation of input fields
     */
    var isValidGitUrl: Boolean?

    /**
     * Validation of input fields
     */
    var isValidGitUser: Boolean?

    /**
     * Validation of input fields
     */
    var isValidGitToken: Boolean?

    /**
     * Validation of input fields
     */
    var gitConnectionCheckingStatus: GitConnectionStatusEnum?

    /**
     * Flag to public project
     */
    var isPublic: Boolean?
}

/**
 * Special enum that stores the value with the result of testing git credentials
 */
enum class GitConnectionStatusEnum {
    CHECKED_NOT_OK,
    CHECKED_OK,
    INTERNAL_SERVER_ERROR,
    NOT_CHECKED,
    VALIDATING,
    ;
}

/**
 * A functional Component for project creation view
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class CreationView : AbstractView<Props, ProjectSaveViewState>(true) {
    private val fieldsMap: MutableMap<InputTypes, String> = mutableMapOf()

    init {
        state.isErrorWithProjectSave = false
        state.errorMessage = ""
        state.gitConnectionCheckingStatus = GitConnectionStatusEnum.NOT_CHECKED

        state.isValidOrganization = true
        state.isValidProjectName = true
        state.isValidGitUrl = true
        state.isValidGitUser = true
        state.isValidGitToken = true
        state.isPublic = true
    }

    private fun changeFields(
        fieldName: InputTypes,
        target: ChangeEvent<Element>,
        isProject: Boolean = true,
    ) {
        val tg = target.target
        val value = when (tg) {
            is HTMLInputElement -> tg.value
            is HTMLSelectElement -> tg.value
            else -> ""
        }
        fieldsMap[fieldName] = value
    }

    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION")
    private fun validateGitConnection() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        val urlArguments =
                "?user=${fieldsMap[InputTypes.GIT_USER]}&token=${fieldsMap[InputTypes.GIT_TOKEN]}&url=${fieldsMap[InputTypes.GIT_URL]}"

        scope.launch {
            setState {
                gitConnectionCheckingStatus = GitConnectionStatusEnum.VALIDATING
            }
            val responseFromCreationProject =
                    get(
                        "$apiUrl/check-git-connectivity-adaptor$urlArguments",
                        headers, loadingHandler = ::classLoadingHandler,
                    )

            if (responseFromCreationProject.ok) {
                if (responseFromCreationProject.text().await().toBoolean()) {
                    setState {
                        gitConnectionCheckingStatus = GitConnectionStatusEnum.CHECKED_OK
                    }
                } else {
                    setState {
                        gitConnectionCheckingStatus = GitConnectionStatusEnum.CHECKED_NOT_OK
                    }
                }
            } else {
                setState {
                    gitConnectionCheckingStatus = GitConnectionStatusEnum.INTERNAL_SERVER_ERROR
                }
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION", "MAGIC_NUMBER")
    private fun saveProject() {
        if (!isValidInput()) {
            return
        }
        val organizationName = fieldsMap[InputTypes.ORGANIZATION_NAME]!!.trim()
        val date = LocalDateTime(1970, Month.JANUARY, 1, 0, 0, 1)
        val newProjectRequest = NewProjectDto(
            Project(
                fieldsMap[InputTypes.PROJECT_NAME]!!.trim(),
                fieldsMap[InputTypes.PROJECT_URL]?.trim(),
                fieldsMap[InputTypes.DESCRIPTION]?.trim(),
                ProjectStatus.CREATED,
                public = state.isPublic!!,
                userId = -1,
                organization = Organization("stub", OrganizationStatus.CREATED, null, date)
            ),
            fieldsMap[InputTypes.ORGANIZATION_NAME]!!.trim(),
            GitDto(
                fieldsMap[InputTypes.GIT_URL]?.trim() ?: "",
                fieldsMap[InputTypes.GIT_USER]?.trim(),
                fieldsMap[InputTypes.GIT_TOKEN]?.trim(),
                fieldsMap[InputTypes.GIT_BRANCH]?.trim()
            ),
        )
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            val responseFromCreationProject =
                    post(
                        "$apiUrl/projects/save",
                        headers,
                        Json.encodeToString(newProjectRequest),
                        loadingHandler = ::classLoadingHandler,
                    )
            if (responseFromCreationProject.ok == true) {
                window.location.href = "${window.location.origin}#/${organizationName.replace(" ", "%20")}/" +
                        newProjectRequest.project.name.replace(" ", "%20")
                window.location.reload()
            } else {
                responseFromCreationProject.text().then {
                    setState {
                        isErrorWithProjectSave = true
                        errorMessage = it
                    }
                }
            }
        }
    }

    /**
     * A little bit ugly method with code duplication due to different states.
     * FixMe: May be it will be possible to optimize it in the future, now we don't have time.
     */
    @Suppress("TOO_LONG_FUNCTION", "SAY_NO_TO_VAR")
    private fun isValidInput(): Boolean {
        var valid = true
        if (fieldsMap[InputTypes.ORGANIZATION_NAME].isNullOrBlank()) {
            setState { isValidOrganization = false }
            valid = false
        } else {
            setState { isValidOrganization = true }
        }

        val projectName = fieldsMap[InputTypes.PROJECT_NAME]
        if (projectName.isInvalid(64)) {
            setState { isValidProjectName = false }
            valid = false
        } else {
            setState { isValidProjectName = true }
        }

        val gitUser = fieldsMap[InputTypes.GIT_USER]
        if (gitUser.isNullOrBlank() || Regex(".*\\s.*").matches(gitUser.trim())) {
            setState { isValidGitUser = false }
        } else {
            setState { isValidGitUser = true }
        }

        val gitToken = fieldsMap[InputTypes.GIT_TOKEN]
        if (gitToken.isNullOrBlank() || Regex(".*\\s.*").matches(gitToken.trim())) {
            setState { isValidGitToken = false }
        } else {
            setState { isValidGitToken = true }
        }

        val gitUrl = fieldsMap[InputTypes.GIT_URL]
        if (gitUrl.isNullOrBlank() || !gitUrl.trim().startsWith("http")) {
            setState { isValidGitUrl = false }
        } else {
            setState { isValidGitUrl = true }
        }
        return valid
    }

    @Suppress(
        "TOO_LONG_FUNCTION",
        "LongMethod",
    )
    override fun ChildrenBuilder.render() {
        runErrorModal(
            state.isErrorWithProjectSave,
            "Error appeared during project creation",
            state.errorMessage
        ) {
            setState { isErrorWithProjectSave = false }
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
                            div {
                                className = ClassName("p-5 text-center")
                                h1 {
                                    className = ClassName("h4 text-gray-900 mb-4")
                                    +"Create new test project"
                                }
                                div {
                                    button {
                                        type = ButtonType.button
                                        className = ClassName("btn btn-primary mb-2")
                                        a {
                                            className = ClassName("text-light")
                                            href = "#/createOrganization/"
                                            +"Add new organization"
                                        }
                                    }
                                }
                                form {
                                    className = ClassName("needs-validation")
                                    div {
                                        className = ClassName("row g-3")
                                        selectFormRequired {
                                            form = InputTypes.ORGANIZATION_NAME
                                            validInput = state.isValidOrganization!!
                                            classes = "col-md-6 pl-0 pl-2 pr-2"
                                            text = "Organization"
                                            onChangeFun = ::changeFields
                                        }
                                        inputTextFormRequired(InputTypes.PROJECT_NAME, state.isValidProjectName!!, "col-md-6 pl-2 pr-2", "Tested tool name", true) {
                                            changeFields(InputTypes.PROJECT_NAME, it)
                                        }
                                        inputTextFormOptional(InputTypes.PROJECT_URL, "col-md-6 pr-0 mt-3", "Tested Tool Website") {
                                            changeFields(InputTypes.PROJECT_URL, it)
                                        }
                                        inputTextFormOptional(
                                            InputTypes.GIT_URL,
                                            "col-md-6 mt-3 pl-0",
                                            "Test Suite Git URL"
                                        ) {
                                            changeFields(InputTypes.GIT_URL, it, false)
                                        }

                                        div {
                                            className = ClassName("col-md-12 mt-3 mb-3 pl-0 pr-0")
                                            label {
                                                className = ClassName("form-label")
                                                asDynamic()["for"] = InputTypes.DESCRIPTION.name
                                                +"Description"
                                            }
                                            div {
                                                className = ClassName("input-group has-validation")
                                                textarea {
                                                    className = ClassName("form-control")
                                                    onChange = {
                                                        fieldsMap[InputTypes.DESCRIPTION] = it.target.value
                                                    }
                                                    ariaDescribedBy = "${InputTypes.DESCRIPTION.name}Span"
                                                    rows = 2
                                                    id = InputTypes.DESCRIPTION.name
                                                    required = false
                                                }
                                            }
                                        }

                                        div {
                                            className = ClassName("col-md-12 mt-3 border-top")
                                            p {
                                                className = ClassName("mx-auto mt-2")
                                                +"Provide Credentials if your repo with Test Suites is private:"
                                            }
                                        }

                                        inputTextFormOptional(InputTypes.GIT_USER, "col-md-6 mt-1", "Git Username") {
                                            changeFields(InputTypes.GIT_USER, it, false)
                                        }
                                        inputTextFormOptional(InputTypes.GIT_TOKEN, "col-md-6 mt-1 pr-0", "Git Token") {
                                            changeFields(InputTypes.GIT_TOKEN, it, false)
                                        }

                                        div {
                                            className = ClassName("col-md-12 mt-3 mb-3 pl-2 pr-0 row")
                                            label {
                                                className = ClassName("text-xs")
                                                fontAwesomeIcon(icon = faQuestionCircle)
                                                asDynamic()["data-toggle"] = "tooltip"
                                                asDynamic()["data-placement"] = "top"
                                                title = "Private projects are visible for user, organization admins and selected users, " +
                                                        "while public ones are visible for everyone."
                                            }
                                            div {
                                                className = ClassName("col-5 text-left align-self-center")
                                                +"Project visibility:"
                                            }
                                            form {
                                                className = ClassName("col-7 form-group row d-flex justify-content-around")
                                                div {
                                                    className = ClassName("form-check-inline")
                                                    input {
                                                        className = ClassName("form-check-input")
                                                        defaultChecked = state.isPublic!!
                                                        name = "projectVisibility"
                                                        type = InputType.radio
                                                        id = "isProjectPublicSwitch"
                                                        value = "true"
                                                    }
                                                    label {
                                                        className = ClassName("form-check-label")
                                                        htmlFor = "isProjectPublicSwitch"
                                                        +"Public"
                                                    }
                                                }
                                                div {
                                                    className = ClassName("form-check-inline")
                                                    input {
                                                        className = ClassName("form-check-input")
                                                        defaultChecked = !state.isPublic!!
                                                        name = "projectVisibility"
                                                        type = InputType.radio
                                                        id = "isProjectPrivateSwitch"
                                                        value = "false"
                                                    }
                                                    label {
                                                        className = ClassName("form-check-label")
                                                        htmlFor = "isProjectPrivateSwitch"
                                                        +"Private"
                                                    }
                                                }
                                                onChange = {
                                                    setState {
                                                        isPublic = (it.target as HTMLInputElement).value.toBoolean()
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    button {
                                        type = ButtonType.button
                                        className = ClassName("btn btn-info mt-4 mr-3")
                                        +"Create test project"
                                        onClick = { saveProject() }
                                    }
                                    button {
                                        type = ButtonType.button
                                        className = ClassName("btn btn-success mt-4 ml-3")
                                        +"Validate connection"
                                        onClick = { validateGitConnection() }
                                    }
                                    div {
                                        className = ClassName("row justify-content-center")
                                        when (state.gitConnectionCheckingStatus) {
                                            GitConnectionStatusEnum.CHECKED_NOT_OK ->
                                                createDiv(
                                                    "invalid-feedback d-block",
                                                    "Validation failed: please check your git URL and credentials"
                                                )
                                            GitConnectionStatusEnum.CHECKED_OK ->
                                                createDiv("valid-feedback d-block", "Successful validation of git configuration")
                                            GitConnectionStatusEnum.NOT_CHECKED ->
                                                createDiv("invalid-feedback d-block", "")
                                            GitConnectionStatusEnum.INTERNAL_SERVER_ERROR ->
                                                createDiv("invalid-feedback d-block", "Internal server error during git validation")
                                            GitConnectionStatusEnum.VALIDATING ->
                                                div {
                                                    className = ClassName("spinner-border spinner-border-sm mt-3")
                                                    role = "status".unsafeCast<AriaRole>()
                                                }
                                            else -> {
                                                // do nothing
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ChildrenBuilder.createDiv(blockName: String, text: String) =
            div {
                className = ClassName("$blockName mt-2")
                +text
            }

    companion object : RStatics<Props, ProjectSaveViewState, CreationView, Context<RequestStatusContext>>(CreationView::class) {
        init {
            contextType = requestStatusContext
        }
    }
}
