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
import react.RState
import react.child
import react.dom.a
import react.dom.attrs
import react.dom.button
import react.dom.defaultValue
import react.dom.div
import react.dom.h1
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
import org.cqfn.save.testsuite.TestSuiteDto

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
external interface ProjectViewState : RState {
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
    var isErrorOpen: Boolean

    /**
     * Error label
     */
    var errorLabel: String

    /**
     * Flag to handle loading
     */
    var isLoading: Boolean

    /**
     * Selected sdk
     */
    var selectedSdk: String

    /**
     * Selected version
     */
    var selectedSdkVersion: String
}

/**
 * A functional RComponent for project view
 * Each modal opening call re render full page, that why we need to use state for all fields
 *
 * @return a functional component
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
@Suppress("CUSTOM_GETTERS_SETTERS")
class ProjectView : RComponent<ProjectExecutionRouteProps, ProjectViewState>() {
    private var testTypesList = listOf<TestSuiteDto>()
    private var pathToProperty: String? = null
    private var gitUrlFromInputField: String? = null
    private val selectedTypes: MutableList<String> = mutableListOf()
    private var gitDto: GitDto? = null

    private var numberOpenningCard: Int = 1  // 1 - first card, 2 - second card, 3 - none card was opened
    private var project = Project("stub", "stub", "stub", "stub")
    private val allSdks = sdks
    private lateinit var responseFromExecutionRequest: Response

    init {
        state.isErrorOpen = false
        state.errorMessage = ""
        state.errorLabel = ""

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
            testTypesList = get("http://localhost:5000/allStandardTestSuites", headers)
                .decodeFromJsonString()
            setState { isLoading = false }
        }
    }

    @Suppress("ComplexMethod", "TOO_LONG_FUNCTION")
    private fun submitExecutionRequest() {
        if (numberOpenningCard == 0) {
            setState {
                isErrorOpen = true
                errorLabel = "No project type"
                errorMessage = "Please choose one of the project types"
            }
        } else if (numberOpenningCard == 1) {
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
        formData.append("execution", Blob(arrayOf(JSON.stringify(request)), BlobPropertyBag("application/json")))
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
        val jsonExecution = JSON.stringify(executionRequest)
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
            child(cardComponent {
                div("text-center") {
                    attrs.id = "accordion"
                    div("card shadow mb-4") {
                        div("card-header") {
                            attrs.id = "headingFirst"
                            button(classes = "btn btn-link collapsed") {
                                attrs["data-toggle"] = "collapse"
                                attrs["data-target"] = "#collapseFirst"
                                attrs["aria-expanded"] = "true"
                                attrs["aria-controls"] = "collapseFirst"
                                h6("m-0 font-weight-bold text-primary") {
                                    +"Upload project as git url"
                                }
                                attrs.onClickFunction = {
                                    if (numberOpenningCard == 1) {
                                        numberOpenningCard = 0
                                    } else {
                                        numberOpenningCard = 1
                                    }
                                }
                            }
                        }
                        // Collapse card to load git url
                        div {
                            attrs.classes = if (numberOpenningCard == 1) setOf("collapse", "show") else setOf("collapse")
                            attrs.id = "collapseFirst"
                            attrs["aria-labelledby"] = "headingFirst"
                            attrs["data-parent"] = "#accordion"
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
                    }

                    // Collapse card to load binary file
                    div("card shadow mb-4") {
                        div("card-header") {
                            attrs.id = "headingSecond"
                            button(classes = "btn btn-link collapsed") {
                                attrs["data-toggle"] = "collapse"
                                attrs["data-target"] = "#collapseSecond"
                                attrs["aria-expanded"] = "true"
                                attrs["aria-controls"] = "collapseSecond"
                                h6("m-0 font-weight-bold text-primary") {
                                    +"Upload project as binary file"
                                }
                                attrs.onClickFunction = {
                                    if (numberOpenningCard == 2) {
                                        numberOpenningCard = 0
                                    } else {
                                        numberOpenningCard = 2
                                    }
                                }
                            }
                        }
                        div {
                            attrs.classes = if (numberOpenningCard == 2) setOf("collapse", "show") else setOf("collapse")
                            attrs.id = "collapseSecond"
                            attrs["aria-labelledby"] = "headingSecond"
                            attrs["data-parent"] = "#accordion"
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
                            }
                            div {
                                testTypesList
                                    .map { it.name }
                                    .chunked(TEST_SUITE_ROW)
                                    .forEach { rowTypes ->
                                        div("row") {
                                            rowTypes.forEach { typeName ->
                                                div("col") {
                                                    +typeName
                                                    input(type = InputType.checkBox, classes = "ml-3") {
                                                        attrs.defaultChecked = selectedTypes.contains(typeName)
                                                        attrs.onClickFunction = {
                                                            if (selectedTypes.contains(typeName)) {
                                                                selectedTypes.remove(typeName)
                                                            } else {
                                                                selectedTypes.add(typeName)
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
                            attrs.classes = if (state.selectedSdk == "Default") setOf("d-none") else setOf()
                            div("d-inline-block") {
                                h6 {
                                    +"Version:"
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
                    header = "Run tests"
                    leftBorderColor = "primary"
                }
            }

            child(cardComponent {
                p("small") {
                    +"Name: ${project.name}"
                }
                p("small") {
                    +"Description: ${project.description}"
                }
                p("small") {
                    button(classes = "btn btn-link btn-sm") {
                        +"Latest execution"
                        attrs.onClickFunction = {
                            GlobalScope.launch {
                                val headers = Headers().apply { set("Accept", "application/json") }
                                val response = get("${window.location.origin}/latestExecution?name=${project.name}&owner=${project.owner}", headers)
                                if (!response.ok) {
                                    setState {
                                        errorLabel = "Failed to fetch latest execution"
                                        errorMessage = "Failed to fetch latest execution: ${response.status} ${response.statusText}"
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
                p("small") {
                    a(href = "#/${project.owner}/${project.name}/history", classes = "btn btn-link btn-sm") {
                        +"Execution history"
                    }
                }
            }) {
                attrs {
                    header = "Project info"
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
