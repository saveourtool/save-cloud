/**
 * A view with project details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package org.cqfn.save.frontend.components.views

import org.cqfn.save.domain.FileInfo
import org.cqfn.save.domain.Sdk
import org.cqfn.save.domain.getSdkVersions
import org.cqfn.save.domain.toSdk
import org.cqfn.save.entities.*
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.frontend.components.basic.*
import org.cqfn.save.frontend.externals.fontawesome.faCalendarAlt
import org.cqfn.save.frontend.externals.fontawesome.faHistory
import org.cqfn.save.frontend.externals.fontawesome.faQuestionCircle
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.externals.modal.modal
import org.cqfn.save.frontend.utils.*
import org.cqfn.save.testsuite.TestSuiteDto

import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.xhr.FormData
import react.PropsWithChildren
import react.RBuilder
import react.RComponent
import react.State
import react.dom.*
import react.setState

import kotlinx.browser.document
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
     * Message of warning
     */
    var confirmMessage: String

    /**
     * Flag to handle confirm Window
     */
    var isConfirmWindowOpen: Boolean?

    /**
     * Label of confirm Window
     */
    var confirmLabel: String

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
    /**
     * Sumbit button was pressed
     */
    var isSubmitButtonPressed: Boolean?

    /**
     * state for the creation of unified confirmation logic
     */
    var confirmationType: ConfirmationType
}

/**
 * enum that stores types of confirmation windows
 */
enum class ConfirmationType {
    DELETE_CONFIRM,
    NO_BINARY_CONFIRM,
    NO_CONFIRM,
    ;
}

/**
 * A Component for project view
 * Each modal opening call causes re-render of the whole page, that's why we need to use state for all fields
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
class ProjectView : RComponent<ProjectExecutionRouteProps, ProjectViewState>() {
    private var standardTestSuites: List<TestSuiteDto> = emptyList()
    private var testRootPath: String? = null
    private var gitUrlFromInputField: String? = null
    private val selectedTypes: MutableList<String> = mutableListOf()
    private var gitDto: GitDto? = null
    private var project = Project("stub", "stub", "stub", "stub", ProjectStatus.CREATED)
    private val projectInformation = mutableMapOf(
        "Tested tool name: " to "",
        "Description: " to "",
        "Tested tool Url: " to "",
        "Test project owner: " to ""
    )
    private lateinit var responseFromDeleteProject: Response

    init {
        state.confirmationType = ConfirmationType.NO_CONFIRM

        state.isErrorOpen = false
        state.isSubmitButtonPressed = false

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
        // URL is required in all cases, the processing should not be done without it
        if (gitUrlFromInputField.isNullOrBlank()) {
            return
        }

        if (state.isFirstTypeUpload == true) {
            gitUrlFromInputField?.let {
                val newGitDto = GitDto(url = it)
                submitExecutionRequestWithCustomTests(newGitDto)
            } ?: gitDto?.let {
                submitExecutionRequestWithCustomTests(it)

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

            submitExecutionRequestWithStandardTests()
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun submitExecutionRequestWithStandardTests() {
        val headers = Headers()
        val formData = FormData()
        val selectedSdk = "${state.selectedSdk}:${state.selectedSdkVersion}".toSdk()
        val request = ExecutionRequestForStandardSuites(project, selectedTypes, selectedSdk)
        formData.appendJson("execution", request)
        state.files.forEach {
            formData.appendJson("file", it)
        }
        submitRequest("/executionRequestStandardTests", headers, formData)
    }

    private fun submitExecutionRequestWithCustomTests(correctGitDto: GitDto) {
        val selectedSdk = "${state.selectedSdk}:${state.selectedSdkVersion}".toSdk()
        val formData = FormData()
        val executionRequest = testRootPath?.let {
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
            executionId = null
        )
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
        runConfirmWindowModal(state.isConfirmWindowOpen, state.confirmLabel, state.confirmMessage, { setState { isConfirmWindowOpen = false } }) {
            deleteProjectBuilder()
            setState { isConfirmWindowOpen = false }
        }
        runLoadingModal()
        // Page Heading
        div("d-sm-flex align-items-center justify-content-center mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +"Project ${project.name}"
            }
        }

        div("row justify-content-center") {
            // ===================== LEFT COLUMN =======================================================================
            div("col-2 mr-3") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Testing types"
                }

                child(cardComponent {
                    div("text-left") {
                        div("mr-2") {
                            button(type = ButtonType.button) {
                                attrs.classes =
                                        if (state.isFirstTypeUpload == true) {
                                            setOf("btn", "btn-primary")
                                        } else {
                                            setOf(
                                                "btn",
                                                "btn-outline-primary"
                                            )
                                        }
                                attrs.onClickFunction = {
                                    setState {
                                        isFirstTypeUpload = true
                                    }
                                }
                                +"Run your tool with your specific tests from git"
                            }
                        }
                        div("mt-3 mr-2") {
                            button(type = ButtonType.button, classes = "btn btn-link collapsed") {
                                attrs.classes =
                                        if (state.isFirstTypeUpload == true) {
                                            setOf("btn", "btn-outline-primary")
                                        } else {
                                            setOf(
                                                "btn",
                                                "btn-primary"
                                            )
                                        }
                                attrs.onClickFunction = {
                                    setState {
                                        isFirstTypeUpload = false
                                    }
                                }
                                +"Run your tool with standard test suites"
                            }
                        }
                    }
                })
            }
            // ===================== MIDDLE COLUMN =====================================================================
            div("col-4") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Test configuration"
                }

                child(
                    fileUploader(
                        onFileSelect = { element ->
                            setState {
                                val availableFile = availableFiles.first { it.name == element.value }
                                files.add(availableFile)
                                availableFiles.remove(availableFile)
                            }
                        },
                        onFileRemove = {
                            setState {
                                files.remove(it)
                                this.availableFiles.add(it)
                            }
                        },
                        onFileInput = { htmlInputElement ->
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
                        },
                        onExecutableChange = { selectedFile, checked ->
                            setState {
                                files[files.indexOf(selectedFile)] = selectedFile.copy(isExecutable = checked)
                            }
                        }
                    )
                ) {
                    attrs.isSubmitButtonPressed = state.isSubmitButtonPressed
                    attrs.files = state.files
                    attrs.availableFiles = state.availableFiles
                    attrs.confirmationType = state.confirmationType
                }

                h6(classes = "d-inline mr-3") {
                    +"2. Select the SDK if needed:"
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

                h6(classes = "d-inline") {
                    +"3. Specify test-resources that will be used for testing:"
                }

                child(cardComponent {
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

                        div("card-body ") {
                            div("input-group-sm mb-3") {
                                div("row") {
                                    sup("tooltip-and-popover") {
                                        fontAwesomeIcon(icon = faQuestionCircle)
                                        attrs["tooltip-placement"] = "top"
                                        attrs["tooltip-title"] = ""
                                        attrs["popover-placement"] = "left"
                                        attrs["popover-title"] =
                                                "Use the following link to read more about save format:"
                                        attrs["popover-content"] =
                                                "<a href =\"https://github.com/cqfn/save/blob/main/README.md\" > Save core README </a>"
                                        attrs["data-trigger"] = "focus"
                                        attrs["tabindex"] = "0"
                                    }
                                    h6(classes = "d-inline ml-2") {
                                        +"Git Url of your test suites (in save format):"
                                    }
                                }
                                div("input-group-prepend") {
                                    input(type = InputType.text) {
                                        attrs["class"] =
                                                if (gitUrlFromInputField.isNullOrBlank() && state.isSubmitButtonPressed!!) {
                                                    "form-control is-invalid"
                                                } else {
                                                    "form-control"
                                                }
                                        attrs {
                                            gitUrlFromInputField?.let {
                                                defaultValue = it
                                            } ?: gitDto?.url?.let {
                                                defaultValue = it
                                            }
                                            placeholder = "https://github.com/my-project"
                                            onChangeFunction = {
                                                val target = it.target as HTMLInputElement
                                                gitUrlFromInputField = target.value
                                            }
                                        }
                                    }
                                }
                            }

                            div("input-group-sm") {
                                div("row") {
                                    sup("tooltip-and-popover") {
                                        fontAwesomeIcon(icon = faQuestionCircle)
                                        attrs["tooltip-placement"] = "top"
                                        attrs["tooltip-title"] = ""
                                        attrs["popover-placement"] = "left"
                                        attrs["popover-title"] = "Relative path to the root directory with tests"
                                        attrs["popover-content"] = TEST_ROOT_DIR_HINT
                                        attrs["data-trigger"] = "focus"
                                        attrs["tabindex"] = "0"
                                    }
                                    h6(classes = "d-inline ml-2") {
                                        +"Relative path to the root directory with tests in the repo:"
                                    }
                                }
                                div("input-group-prepend") {
                                    input(type = InputType.text, name = "itemText") {
                                        key = "itemText"
                                        attrs.set("class", "form-control")
                                        attrs {
                                            testRootPath?.let {
                                                value = it
                                            }
                                            placeholder = "leave empty if tests are in the repository root"
                                            onChangeFunction = {
                                                val target = it.target as HTMLInputElement
                                                testRootPath = target.value
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
                            child(
                                checkBoxGrid(
                                    standardTestSuites
                                )
                            ) {
                                attrs.selectedOptions = selectedTypes
                                attrs.rowSize = TEST_SUITE_ROW
                            }
                        }
                    }

                    div("d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-primary") {
                            attrs.onClickFunction = { submitWithValidation() }
                            +"Test the tool now"
                        }
                    }
                })
            }
            // ===================== RIGHT COLUMN ======================================================================
            div("col-3 ml-2") {
                div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                    +"Information"
                    button(classes = "btn btn-link text-xs text-muted text-left p-1 ml-2") {
                        +"Edit"
                        attrs.onClickFunction = {
                            turnEditMode(off = false)
                        }
                    }
                }

                child(cardComponent {
                    val newProjectInformation: MutableMap<String, String> = mutableMapOf()
                    form {
                        projectInformation.putAll(
                            projectInformation.keys.zip(listOf(project.name, project.description ?: "", project.url ?: "", project.owner))
                        )
                        projectInformation
                            .forEach { (header, text) ->
                                div("control-group form-inline") {
                                    label(classes = "control-label col-auto") {
                                        +header
                                    }
                                    div("controls col-auto") {
                                        input(InputType.text, classes = "form-control-plaintext") {
                                            attrs.id = header
                                            attrs.placeholder = text
                                            attrs.disabled = true
                                            attrs {
                                                onChangeFunction = {
                                                    val tg = it.target as HTMLInputElement
                                                    val newValue = tg.value
                                                    newProjectInformation[header] = newValue
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    }

                    button(classes = "btn btn-success text-xs p-1 ml-2") {
                        +"Save"
                        attrs.id = "Save new project info"
                        attrs.hidden = true
                        attrs.onClickFunction = {
                            newProjectInformation.forEach { (key, value) ->
                                projectInformation[key] = value
                                (document.getElementById(key) as HTMLInputElement).value = value
                            }
                            updateProjectBuilder(projectInformation)
                            turnEditMode(off = true)
                        }
                    }

                    button(classes = "btn btn-secondary text-xs p-1 ml-5") {
                        +"Cancel"
                        attrs.id = "Cancel"
                        attrs.hidden = true
                        attrs.onClickFunction = {
                            projectInformation.forEach { (key, value) ->
                                (document.getElementById(key) as HTMLInputElement).value = value
                            }
                            turnEditMode(off = true)
                        }
                    }

                    div("ml-3 mt-2 align-items-left justify-content-between") {
                        fontAwesomeIcon(icon = faHistory)

                        button(classes = "btn btn-link text-left") {
                            +"Latest Execution"
                            attrs.onClickFunction = {
                                GlobalScope.launch {
                                    switchToLatestExecution()
                                }
                            }
                        }
                    }
                    div("ml-3 align-items-left") {
                        fontAwesomeIcon(icon = faCalendarAlt)
                        a(
                            href = "#/${project.owner}/${project.name}/history",
                            classes = "btn btn-link text-left"
                        ) {
                            +"Execution History"
                        }
                    }
                    div("ml-3 d-sm-flex align-items-left justify-content-between mt-2") {
                        button(type = ButtonType.button, classes = "btn btn-block btn-danger") {
                            attrs.onClickFunction = {
                                deleteProject()
                            }
                            +"Delete project"
                        }
                    }
                })
            }
        }
    }

    private fun turnEditMode(off: Boolean) {
        projectInformation.keys.forEach {
            (document.getElementById(it) as HTMLInputElement).disabled = off
        }
        (document.getElementById("Save new project info") as HTMLButtonElement).hidden = off
        (document.getElementById("Cancel") as HTMLButtonElement).hidden = off
    }

    private fun RBuilder.runLoadingModal() = modal {
        attrs {
            isOpen = state.isLoading
            contentLabel = "Loading"
        }
        div("d-flex justify-content-center mt-4") {
            div("spinner-border text-primary spinner-border-lg") {
                attrs.role = "status"
                span("sr-only") {
                    +"Loading..."
                }
            }
        }
    }

    /**
     * In some cases scripts and binaries can be uploaded to a git repository, so users won't be providing or uploading
     * binaries. For this case we should open a window, so user will need to click a check box, so he will confirm that
     * he understand what he is doing.
     */
    private fun submitWithValidation() {
        setState {
            isSubmitButtonPressed = true
        }
        when {
            // url was not provided
            gitUrlFromInputField.isNullOrBlank() && state.isFirstTypeUpload!! -> setState {
                isErrorOpen = true
                errorMessage = "Git Url with test suites in save format was not provided. It is required for processing."
                errorLabel = "Git Url"
            }
            // no binaries were provided
            state.files.isEmpty() -> setState {
                confirmationType = ConfirmationType.NO_BINARY_CONFIRM
                isConfirmWindowOpen = true
                confirmLabel = "Single binary confirmation"
                confirmMessage = "You have not provided any files related to your tested tool." +
                        " If these files were uploaded to your repository - press OK, otherwise - please upload these files using 'Upload files' button."
            }
            // everything is in place, can proceed
            else -> submitExecutionRequest()
        }
    }

    private fun deleteProject() {
        project.status = ProjectStatus.DELETED

        setState {
            confirmationType = ConfirmationType.DELETE_CONFIRM
            isConfirmWindowOpen = true
            confirmLabel = ""
            confirmMessage = "Are you sure you want to delete this project?"
        }
    }

    private fun updateProjectBuilder(projectInfo: Map<String, String>) {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        val (name, description, url, owner) = projectInfo.values.toList()
        project.name = name
        project.description = description
        project.url = url
        project.owner = owner
        GlobalScope.launch {
            post("${window.location.origin}/updateProject", headers, Json.encodeToString(project))
        }
    }

    private fun deleteProjectBuilder() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        GlobalScope.launch {
            responseFromDeleteProject =
                    post("${window.location.origin}/updateProject", headers, Json.encodeToString(project))
        }.invokeOnCompletion {
            if (responseFromDeleteProject.ok) {
                window.location.href = "${window.location.origin}/"
            } else {
                responseFromDeleteProject.text().then {
                    setState {
                        errorMessage = it
                    }
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
                        "Failed to fetch latest execution: [${response.status}] ${response.statusText}"
                isErrorOpen = true
            }
        } else {
            val latestExecutionId = response
                .decodeFromJsonString<ExecutionDto>()
                .id
            window.location.href = "${window.location}/history/execution/$latestExecutionId"
        }
    }

    private suspend fun getFilesList() = get("${window.location.origin}/files/list", Headers())
        .unsafeMap {
            it.decodeFromJsonString<List<FileInfo>>()
        }

    companion object {
        const val TEST_ROOT_DIR_HINT = """
            The path you are providing should be relative to the root directory of your repository.
            This directory should contain <a href = "https://github.com/cqfn/save#how-to-configure"> save.properties </a>
            or <a href = "https://github.com/cqfn/save#-savetoml-configuration-file">save.toml</a> files. 
            For example, if the URL to your repo with tests is: 
            <a href ="https://github.com/cqfn/save/">https://github.com/cqfn/save</a>, then 
            you need to specify the following directory with 'save.toml': 
            <a href ="https://github.com/cqfn/save/tree/main/examples/kotlin-diktat">examples/kotlin-diktat/</a>. 
 
            Please note, that the tested tool and it's resources will be copied to this directory before the run.
            """
        const val TEST_SUITE_ROW = 4
    }
}
