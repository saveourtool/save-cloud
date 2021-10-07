/**
 * A view with project details
 */

package org.cqfn.save.frontend.components.views

import org.cqfn.save.domain.FileInfo
import org.cqfn.save.domain.Sdk
import org.cqfn.save.domain.getSdkVersions
import org.cqfn.save.domain.toSdk
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.frontend.components.basic.cardComponent
import org.cqfn.save.frontend.components.basic.checkBoxGrid
import org.cqfn.save.frontend.components.basic.fileUploader
import org.cqfn.save.frontend.components.basic.sdkSelection
import org.cqfn.save.frontend.externals.modal.modal
import org.cqfn.save.frontend.utils.appendJson
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.getProject
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.runErrorModal
import org.cqfn.save.frontend.utils.unsafeMap
import org.cqfn.save.testsuite.TestSuiteDto

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.xhr.FormData
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.dom.a
import react.dom.attrs
import react.dom.button
import react.dom.defaultValue
import react.dom.div
import react.dom.h1
import react.dom.h6
import react.dom.i
import react.dom.input
import react.dom.p
import react.dom.span
import react.setState

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.role
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * `Props` retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectExecutionRouteProps : PropsWithChildren {
    var owner: String
    var name: String
}

/**
 * [State] of project view component
 */
external interface ProjectViewState : State {
    /**
     * Files required for tests execution for this project
     */
    var files: MutableList<FileInfo>

    /**
     * Files that are available on server side
     */
    var availableFiles: MutableList<FileInfo>

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
 * A Component for project view
 * Each modal opening call causes re-render of the whole page, that's why we need to use state for all fields
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ProjectView : RComponent<ProjectExecutionRouteProps, ProjectViewState>() {
    private var standardTestSuites: List<TestSuiteDto> = emptyList()
    private var pathToProperty: String? = null
    private var gitUrlFromInputField: String? = null
    private val selectedTypes: MutableList<String> = mutableListOf()
    private var gitDto: GitDto? = null
    private var project = Project("stub", "stub", "stub", "stub")

    init {
        state.isErrorOpen = false
        state.errorMessage = ""
        state.errorLabel = ""

        state.isFirstTypeUpload = true

        state.isLoading = true

        state.files = mutableListOf()
        state.availableFiles = mutableListOf()
        state.selectedSdk = Sdk.Default.name
        state.selectedSdkVersion = Sdk.Default.version
    }

    override fun componentDidMount() {
        GlobalScope.launch {
            project = getProject(props.name, props.owner)
            val jsonProject = Json.encodeToString(project)
            val headers = Headers().apply {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
            }
            gitDto = post("${window.location.origin}/getGit", headers, jsonProject)
                .decodeFromJsonString<GitDto>()
            standardTestSuites = get("${window.location.origin}/allStandardTestSuites", headers)
                .decodeFromJsonString()

            val availableFiles = getFilesList()
            setState {
                this.availableFiles.clear()
                this.availableFiles.addAll(availableFiles)
                isLoading = false
            }
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
            if (state.files.isEmpty()) {
                setState {
                    isErrorOpen = true
                    errorLabel = "No files have been selected"
                    errorMessage = "Please provide files necessary for execution"
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
        formData.appendJson("execution", request)
        state.files.forEach {
            formData.appendJson("file", it)
        }
        submitRequest("/submitExecutionRequestBin", headers, formData)
    }

    private fun submitExecutionRequestGit(correctGitDto: GitDto) {
        val selectedSdk = "${state.selectedSdk}:${state.selectedSdkVersion}".toSdk()
        val formData = FormData()
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
        formData.appendJson("executionRequest", executionRequest)
        state.files.forEach {
            formData.appendJson("file", it)
        }
        submitRequest("/submitExecutionRequest", Headers(), formData)
    }

    private fun submitRequest(url: String, headers: Headers, body: dynamic) {
        setState {
            isLoading = true
        }
        GlobalScope.launch {
            val response = post(window.location.origin + url, headers, body)
            if (!response.ok) {
                response.text().then { text ->
                    setState {
                        isErrorOpen = true
                        errorLabel = "Error from backend"
                        errorMessage = "${response.statusText}: $text"
                    }
                }
            } else {
                window.location.href = "${window.location}/history"
            }
        }
            .invokeOnCompletion {
                setState {
                    isLoading = false
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
                                                +"Path to the root directory with tests in repo: "
                                            }
                                        }
                                        div("d-inline-block ml-2") {
                                            input(type = InputType.text, name = "itemText") {
                                                key = "itemText"
                                                attrs {
                                                    pathToProperty?.let {
                                                        value = it
                                                    }
                                                    placeholder = "sub-path (can be empty)"
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
                                    child(checkBoxGrid(
                                        standardTestSuites
                                    )) {
                                        attrs.selectedOptions = selectedTypes
                                        attrs.rowSize = TEST_SUITE_ROW
                                    }
                                }
                            }
                        }

                        child(fileUploader({ element ->
                            setState {
                                val availableFile = availableFiles.first { it.name == element.value }
                                files.add(availableFile)
                                availableFiles.remove(availableFile)
                            }
                        }, {
                            setState {
                                files.remove(it)
                                this.availableFiles.add(it)
                            }
                        }) { htmlInputElement ->
                            GlobalScope.launch {
                                setState {
                                    isLoading = true
                                }
                                htmlInputElement.files!!.asList().forEach { file ->
                                    val response: FileInfo = post(
                                        "${window.location.origin}/files/upload",
                                        Headers(),
                                        FormData().apply {
                                            append("file", file)
                                        }
                                    )
                                        .decodeFromJsonString()
                                    setState {
                                        // add only to selected files so that this entry isn't duplicated
                                        files.add(response)
                                    }
                                }
                                setState {
                                    isLoading = false
                                }
                            }
                        }) {
                            attrs.files = state.files
                            attrs.availableFiles = state.availableFiles
                        }
                        child(sdkSelection({
                            setState {
                                selectedSdk = it.value
                                selectedSdkVersion = selectedSdk.getSdkVersions().first()
                            }
                        }, {
                            setState { selectedSdkVersion = it.value }
                        })) {
                            attrs.selectedSdk = state.selectedSdk
                            attrs.selectedSdkVersion = state.selectedSdkVersion
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
                            i { +"Name: " }
                            +project.name
                        }
                    }
                    div("ml-3") {
                        h6("d-inline") {
                            i { +"Description: " }
                            +(project.description ?: "")
                        }
                    }
                    p {
                        button(classes = "btn btn-link btn-lg") {
                            +"Latest execution"
                            attrs.onClickFunction = {
                                GlobalScope.launch {
                                    switchToLatestExecution()
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

    private suspend fun switchToLatestExecution() {
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

    private suspend fun getFilesList() = get("${window.location.origin}/files/list", Headers())
        .unsafeMap {
            it.decodeFromJsonString<List<FileInfo>>()
        }

    companion object {
        const val TEST_SUITE_ROW = 4
    }
}
