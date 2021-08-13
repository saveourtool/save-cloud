/**
 * A view with project details
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.domain.getSdkVersion
import org.cqfn.save.domain.sdks
import org.cqfn.save.domain.toSdk
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.frontend.components.basic.cardComponent
import org.cqfn.save.frontend.externals.modal.modal
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.getProject
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.runErrorModal
import org.cqfn.save.testsuite.TestSuiteDto

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.get
import org.w3c.xhr.FormData
import react.RBuilder
import react.RComponent
import react.RProps
import react.State
import react.child
import react.dom.a
import react.dom.attrs
import react.dom.button
import react.dom.defaultValue
import react.dom.div
import react.dom.h1
import react.dom.h4
import react.dom.h5
import react.dom.h6
import react.dom.img
import react.dom.input
import react.dom.label
import react.dom.option
import react.dom.p
import react.dom.select
import react.dom.span
import react.dom.strong
import react.setState

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.hidden
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cqfn.save.frontend.components.basic.checkBoxGrid

/**
 * [RProps] retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectExecutionRouteProps : RProps {
    var owner: String
    var name: String
}

/**
 * [RState] of project view component
 */
external interface ProjectViewState : State {
    /**
     * Binary file of project
     */
    var binaryFile: File?

    /**
     * Property file for project
     */
    var propertyFile: File?

    /**
     * Message of error
     */
    var errorMessage: String

    /**
     * Flag to handle error
     */
    var isErrorOpen: Boolean?

    /**
     * Error label
     */
    var errorLabel: String

    /**
     * Flag to handle loading
     */
    var isLoading: Boolean?

    /**
     * Selected sdk
     */
    var selectedSdk: String

    /**
     * Selected version
     */
    var selectedSdkVersion: String

    /**
     * Flag to handle upload type project
     */
    var isFirstTypeUpload: Boolean?
}

/**
 * A functional RComponent for project view
 * Each modal opening call re render full page, that why we need to use state for all fields
 *
 * @return a functional component
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("CUSTOM_GETTERS_SETTERS")
class ProjectView : RComponent<ProjectExecutionRouteProps, ProjectViewState>() {
    private var testTypesList: List<TestSuiteDto> = emptyList()
    private var pathToProperty: String? = null
    private var gitUrlFromInputField: String? = null
    private val selectedTypes: MutableList<String> = mutableListOf()
    private var gitDto: GitDto? = null
    private var project = Project("stub", "stub", "stub", "stub")
    private val allSdks = sdks
    private lateinit var responseFromExecutionRequest: Response

    init {
        state.isErrorOpen = false
        state.errorMessage = ""
        state.errorLabel = ""

        state.isFirstTypeUpload = true

        state.isLoading = true

        state.selectedSdk = "Default"
        state.selectedSdkVersion = "latest"
    }

    override fun componentDidMount() {
        GlobalScope.launch {
            project = getProject(props.name, props.owner)
            val jsonProject = Json.encodeToString(project)
            val headers = Headers().also {
                it.set("Accept", "application/json")
                it.set("Content-Type", "application/json")
            }
            gitDto = post("${window.location.origin}/getGit", headers, jsonProject)
                .decodeFromJsonString<GitDto>()
            testTypesList = get("${window.location.origin}/allStandardTestSuites", headers)
                .decodeFromJsonString()
            setState { isLoading = false }
        }
    }

    @Suppress("ComplexMethod", "TOO_LONG_FUNCTION")
    private fun submitExecutionRequest() {
        if (state.isFirstTypeUpload == true) {
            gitUrlFromInputField?.let {
                val newGitDto = GitDto(url = it)
                submitExecutionRequestGit(newGitDto)
            } ?: gitDto?.let {
                submitExecutionRequestGit(it)
            } ?: setState {
                isErrorOpen = true
                errorLabel = "No git url"
                errorMessage = "Please provide a git url"
            }
        } else {
            if (selectedTypes.isEmpty()) {
                setState {
                    isErrorOpen = true
                    errorLabel = "Both type of project"
                    errorMessage = "Please choose one of type test suites"
                }
                return
            }
            state.binaryFile ?: run {
                setState {
                    isErrorOpen = true
                    errorLabel = "No binary file has been selected"
                    errorMessage = "Please select binary file"
                }
                return
            }
            state.propertyFile ?: run {
                setState {
                    isErrorOpen = true
                    errorLabel = "No property file has been selected"
                    errorMessage = "Please upload save.properties file"
                }
                return
            }
            submitExecutionRequestBinFile()
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun submitExecutionRequestBinFile() {
        val headers = Headers()
        val formData = FormData()
        val selectedSdk = "${state.selectedSdk}:${state.selectedSdkVersion}".toSdk()
        val request = ExecutionRequestForStandardSuites(project, selectedTypes, selectedSdk)
        formData.append("execution", Blob(arrayOf(Json.encodeToString(request)), BlobPropertyBag("application/json")))
        formData.append("property", state.propertyFile!!)
        formData.append("binFile", state.binaryFile!!)
        submitRequest("/submitExecutionRequestBin", headers, formData)
    }

    private fun submitExecutionRequestGit(correctGitDto: GitDto) {
        val selectedSdk = "${state.selectedSdk}:${state.selectedSdkVersion}".toSdk()
        val executionRequest = pathToProperty?.let {
            ExecutionRequest(
                project,
                correctGitDto,
                it,
                selectedSdk,
                null
            )
        } ?: ExecutionRequest(
            project,
            correctGitDto,
            sdk = selectedSdk,
            executionId = null)
        val jsonExecution = Json.encodeToString(executionRequest)
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        submitRequest("/submitExecutionRequest", headers, jsonExecution)
    }

    private fun submitRequest(url: String, headers: Headers, body: dynamic) {
        GlobalScope.launch {
            responseFromExecutionRequest = post(window.location.origin + url, headers, body)
        }.invokeOnCompletion {
            if (responseFromExecutionRequest.ok) {
                window.location.href = "${window.location}/history"
            } else {
                setState {
                    isErrorOpen = true
                    errorLabel = "Error from backend"
                    errorMessage = responseFromExecutionRequest.statusText
                }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
    override fun RBuilder.render() {
        // modal windows are initially hidden
        runErrorModal(state.isErrorOpen, state.errorLabel, state.errorMessage) {
            setState { isErrorOpen = false }
        }
        runLoadingModal()
        // Page Heading
        div("d-sm-flex align-items-center justify-content-between mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"Project ${project.name}"
            }
        }

        div("row") {
            div("col") {
                child(cardComponent {
                    div("text-left") {
                        div {
                            button(type = ButtonType.button) {
                                attrs.classes = if (state.isFirstTypeUpload == true) setOf("btn", "btn-primary") else setOf("btn", "btn-outline-primary")
                                attrs.onClickFunction = {
                                    setState {
                                        isFirstTypeUpload = true
                                    }
                                }
                                +"Upload project as Git"
                            }
                        }
                        div("mt-3") {
                            button(type = ButtonType.button, classes = "btn btn-link collapsed") {
                                attrs.classes = if (state.isFirstTypeUpload == true) setOf("btn", "btn-outline-primary") else setOf("btn", "btn-primary")
                                attrs.onClickFunction = {
                                    setState {
                                        isFirstTypeUpload = false
                                    }
                                }
                                +"Upload project as binary file"
                            }
                        }
                    }
                }) {
                    attrs {
                        header = "Upload types"
                        leftBorderColor = "primary"
                    }
                }
            }
            div("col-6") {
                child(cardComponent {
                    div("text-center") {
                        div("row") {
                            div {
                                attrs.classes = if (state.isFirstTypeUpload == true) {
                                    setOf(
                                        "card",
                                        "shadow",
                                        "mb-4",
                                        "w-100",
                                    )
                                } else {
                                    setOf("d-none")
                                }
                                div("card-body") {
                                    div("pb-3") {
                                        div("d-inline-block") {
                                            h6(classes = "d-inline") {
                                                +"Git url: "
                                            }
                                        }
                                        div("d-inline-block ml-2") {
                                            input(type = InputType.text) {
                                                attrs {
                                                    gitUrlFromInputField?.let {
                                                        defaultValue = it
                                                    } ?: gitDto?.url?.let {
                                                        defaultValue = it
                                                    }
                                                    placeholder = "https://github.com/"
                                                    onChangeFunction = {
                                                        val target = it.target as HTMLInputElement
                                                        gitUrlFromInputField = target.value
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    div("pb-3") {
                                        div("d-inline-block") {
                                            h6(classes = "d-inline") {
                                                +"Path to property file: "
                                            }
                                        }
                                        div("d-inline-block ml-2") {
                                            input(type = InputType.text, name = "itemText") {
                                                key = "itemText"
                                                attrs {
                                                    pathToProperty?.let {
                                                        value = it
                                                    }
                                                    placeholder = "save.properties"
                                                    onChangeFunction = {
                                                        val target = it.target as HTMLInputElement
                                                        pathToProperty = target.value
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            div {
                                attrs.classes = if (state.isFirstTypeUpload == false) {
                                    setOf(
                                        "card",
                                        "shadow",
                                        "mb-4",
                                        "w-100",
                                    )
                                } else {
                                    setOf("d-none")
                                }
                                div("card-body") {
                                    div("mb-3") {
                                        h6(classes = "d-inline mr-3") {
                                            +"Binary file: "
                                        }
                                        div {
                                            label {
                                                input(type = InputType.file) {
                                                    attrs.hidden = true
                                                    attrs {
                                                        onChangeFunction = { event ->
                                                            val target = event.target as HTMLInputElement
                                                            setState { binaryFile = target.files?.let { it[0] } }
                                                        }
                                                    }
                                                }
                                                img(classes = "img-upload", src = "img/upload.svg") {}
                                                strong { +"Upload binary file:" }
                                                +(state.binaryFile?.name ?: "")
                                            }
                                        }
                                    }
                                    div {
                                        h6(classes = "d-inline mr-3") {
                                            +"Properties : "
                                        }
                                        div {
                                            label {
                                                input(type = InputType.file) {
                                                    attrs.hidden = true
                                                    attrs {
                                                        onChangeFunction = { event ->
                                                            val target = event.target as HTMLInputElement
                                                            setState { propertyFile = target.files?.let { it[0] } }
                                                        }
                                                    }
                                                }
                                                img(classes = "img-upload", src = "img/upload.svg") {}
                                                strong { +"Upload property file: " }
                                                +(state.propertyFile?.name ?: "")
                                            }
                                        }
                                    }
                                    child(checkBoxGrid(testTypesList.map { it.name })) {
                                        attrs.selectedTypes = selectedTypes
                                        attrs.rowSize = TEST_SUITE_ROW
                                    }
                                }
                            }
                        }

                        div {
                            div {
                                div("d-inline-block") {
                                    h5 {
                                        +"SDK:"
                                    }
                                }
                                div("d-inline-block ml-2") {
                                    select("form-control form-control mb-3") {
                                        attrs.value = state.selectedSdk
                                        attrs.onChangeFunction = {
                                            val target = it.target as HTMLSelectElement
                                            setState {
                                                selectedSdk = target.value
                                                selectedSdkVersion = selectedSdk.getSdkVersion().first()
                                            }
                                        }
                                        allSdks.forEach {
                                            option {
                                                attrs.value = it
                                                +it
                                            }
                                        }
                                    }
                                }
                            }
                            div {
                                attrs.classes =
                                        if (state.selectedSdk == "Default") setOf("d-none") else setOf("d-inline ml-3")
                                div("d-inline-block") {
                                    h6 {
                                        +"SDK's version:"
                                    }
                                }
                                div("d-inline-block ml-2") {
                                    select("form-select form-select-sm mb-3") {
                                        attrs.value = state.selectedSdkVersion
                                        attrs.onChangeFunction = {
                                            val target = it.target as HTMLSelectElement
                                            setState { selectedSdkVersion = target.value }
                                        }
                                        state.selectedSdk.getSdkVersion().forEach {
                                            option {
                                                attrs.value = it
                                                +it
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        div {
                            button(type = ButtonType.button, classes = "btn btn-primary") {
                                attrs.onClickFunction = { submitExecutionRequest() }
                                +"Run tests now"
                            }
                        }
                    }
                }) {
                    attrs {
                        header = "Run Test"
                        leftBorderColor = "primary"
                    }
                }
            }
            div("col") {
                child(cardComponent {
                    div("ml-3") {
                        h6("d-inline") {
                            +"Name: "
                        }
                        h4("d-inline") {
                            +project.name
                        }
                    }
                    div("ml-3") {
                        h6("d-inline") {
                            +"Description: "
                        }
                        h4("d-inline") {
                            +"${project.description}"
                        }
                    }
                    p {
                        button(classes = "btn btn-link btn-lg") {
                            +"Latest execution"
                            attrs.onClickFunction = {
                                GlobalScope.launch {
                                    val headers = Headers().apply { set("Accept", "application/json") }
                                    val response = get(
                                        "${window.location.origin}/latestExecution?name=${project.name}&owner=${project.owner}",
                                        headers
                                    )
                                    if (!response.ok) {
                                        setState {
                                            errorLabel = "Failed to fetch latest execution"
                                            errorMessage =
                                                    "Failed to fetch latest execution: ${response.status} ${response.statusText}"
                                            isErrorOpen = true
                                        }
                                    } else {
                                        val latestExecutionId = response
                                            .decodeFromJsonString<ExecutionDto>()
                                            .id
                                        window.location.href = "${window.location}/history/$latestExecutionId"
                                    }
                                }
                            }
                        }
                    }
                    p {
                        a(href = "#/${project.owner}/${project.name}/history", classes = "btn btn-link btn-lg") {
                            +"Execution history"
                        }
                    }
                }) {
                    attrs {
                        header = "Information"
                        leftBorderColor = "primary"
                    }
                }
            }
        }
    }

    private fun RBuilder.runLoadingModal() = modal {
        attrs {
            isOpen = state.isLoading
            contentLabel = "Loading"
        }
        div("d-flex justify-content-center") {
            div("spinner-border") {
                attrs.role = "status"
                span("sr-only") {
                    +"Loading..."
                }
            }
        }
    }

    companion object {
        const val TEST_SUITE_ROW = 4
    }
}
