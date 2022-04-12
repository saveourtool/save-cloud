/**
 * A view with project details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS", "LargeClass")

package org.cqfn.save.frontend.components.views

import org.cqfn.save.domain.*
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectStatus
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.frontend.components.basic.TestingType
import org.cqfn.save.frontend.components.basic.cardComponent
import org.cqfn.save.frontend.components.basic.fileUploader
import org.cqfn.save.frontend.components.basic.privacySpan
import org.cqfn.save.frontend.components.basic.projectInfo
import org.cqfn.save.frontend.components.basic.projectSettingsMenu
import org.cqfn.save.frontend.components.basic.projectStatisticMenu
import org.cqfn.save.frontend.components.basic.sdkSelection
import org.cqfn.save.frontend.components.basic.testResourcesSelection
import org.cqfn.save.frontend.components.errorStatusContext
import org.cqfn.save.frontend.externals.fontawesome.faCalendarAlt
import org.cqfn.save.frontend.externals.fontawesome.faEdit
import org.cqfn.save.frontend.externals.fontawesome.faHistory
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.frontend.externals.modal.modal
import org.cqfn.save.frontend.http.getProject
import org.cqfn.save.frontend.utils.ProjectMenuBar
import org.cqfn.save.frontend.utils.apiUrl
import org.cqfn.save.frontend.utils.appendJson
import org.cqfn.save.frontend.utils.decodeFromJsonString
import org.cqfn.save.frontend.utils.get
import org.cqfn.save.frontend.utils.noopResponseHandler
import org.cqfn.save.frontend.utils.post
import org.cqfn.save.frontend.utils.runConfirmWindowModal
import org.cqfn.save.frontend.utils.runErrorModal
import org.cqfn.save.frontend.utils.unsafeMap
import org.cqfn.save.info.UserInfo
import org.cqfn.save.testsuite.TestSuiteDto

import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.fetch.Headers
import org.w3c.fetch.Response
import org.w3c.xhr.FormData
import react.Context
import react.PropsWithChildren
import react.RBuilder
import react.RStatics
import react.State
import react.StateSetter
import react.dom.a
import react.dom.button
import react.dom.div
import react.dom.h1
import react.dom.li
import react.dom.nav
import react.dom.p
import react.dom.span
import react.setState

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.html.ButtonType
import kotlinx.html.classes
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
    var currentUserInfo: UserInfo?
}

/**
 * [State] of project view component
 */
external interface ProjectViewState : State {
    /**
     * Currently loaded for display Project
     */
    var project: Project

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
    var testingType: TestingType

    /**
     * Submit button was pressed
     */
    var isSubmitButtonPressed: Boolean?

    /**
     * State for the creation of unified confirmation logic
     */
    var confirmationType: ConfirmationType

    /**
     * Url to the custom tests
     */
    var gitUrlFromInputField: String

    /**
     * Branch of commit in current repo
     */
    var gitBranchOrCommitFromInputField: String

    /**
     * Execution command for standard mode
     */
    var execCmd: String

    /**
     * Batch size for static analyzer tool in standard mode
     */
    var batchSizeForAnalyzer: String

    /**
     * Directory in the repository where tests are placed
     */
    var testRootPath: String

    /**
     * Selected languages in the list of standard tests
     */
    var selectedLanguageForStandardTests: String

    /**
     * General size of test suite in bytes
     */
    var suiteByteSize: Long

    /**
     * Bytes received by server
     */
    var bytesReceived: Long

    /**
     * Flag to handle uploading a file
     */
    var isUploading: Boolean?

    /**
     * Whether editing of project info is disabled
     */
    var isEditDisabled: Boolean?

    /**
     * project selected menu
     */
    var selectedMenu: ProjectMenuBar?

    /**
     * latest execution id for this project
     */
    var latestExecutionId: Long?

    /**
     * Flag to open menu statistic
     */
    var isOpenMenuStatistic: Boolean?

    /**
     * Flag to open menu settings
     */
    var isOpenMenuSettings: Boolean?
}

/**
 * enum that stores types of confirmation windows for different situations
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
@Suppress("MAGIC_NUMBER")
class ProjectView : AbstractView<ProjectExecutionRouteProps, ProjectViewState>(false) {
    private var standardTestSuites: List<TestSuiteDto> = emptyList()
    private val selectedStandardSuites: MutableList<String> = mutableListOf()
    private var gitDto: GitDto? = null
    private val date = LocalDateTime(1970, Month.JANUARY, 1, 0, 0, 1)
    private val testResourcesSelection = testResourcesSelection(
        updateGitUrlFromInputField = {
            it.preventDefault()
            setState {
                gitUrlFromInputField = (it.target as HTMLInputElement).value
            }
        },
        updateGitBranchOrCommitInputField = {
            setState {
                gitBranchOrCommitFromInputField = (it.target as HTMLInputElement).value
            }
        },
        updateTestRootPath = {
            setState {
                testRootPath = (it.target as HTMLInputElement).value
            }
        },
        setTestRootPathFromHistory = {
            setState {
                testRootPath = it
            }
        },
        setExecCmd = {
            setState {
                execCmd = (it.target as HTMLInputElement).value
            }
        },
        setBatchSize = {
            setState {
                batchSizeForAnalyzer = (it.target as HTMLInputElement).value
            }
        },
        setSelectedLanguageForStandardTests = {
            setState {
                selectedLanguageForStandardTests = it
            }
        }
    )
    private val projectInfo = projectInfo(
        turnEditMode = ::turnEditMode,
        onProjectSave = { draftProject, setDraftProject ->
            if (draftProject != state.project) {
                scope.launch {
                    val response = updateProject(draftProject!!)
                    if (response.ok) {
                        setState {
                            project = draftProject
                        }
                    } else {
                        setState {
                            errorLabel = "Failed to save project info"
                            errorMessage = "Failed to save project info: ${response.status} ${response.statusText}"
                            isErrorOpen = true
                        }
                        // rollback form content
                        setDraftProject(state.project)
                    }
                }
            }
        },
    )

    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    private val projectSettingsMenu = projectSettingsMenu(
        openMenuSettingsFlag = { setState { isOpenMenuSettings = it } },
        deleteProjectCallback = ::deleteProject,
        updateProjectSettings = {
            scope.launch {
                val response = updateProject(it)
                if (response.ok) {
                    setState {
                        project = it
                    }
                } else {
                    setState {
                        errorLabel = "Failed to save project settings"
                        errorMessage = "Failed to save project settings: ${response.status} ${response.statusText}"
                        isErrorOpen = true
                    }
                }
            }
        },
        updateErrorMessage = {
            setState {
                errorLabel = "Failed to save project info"
                errorMessage = "Failed to save project info: ${it.status} ${it.statusText}"
                isErrorOpen = true
            }
        },
    )
    private val projectStatisticMenu = projectStatisticMenu(
        openMenuStatisticFlag = ::openMenuStatisticFlag,
    )
    private val projectInfoCard = cardComponent(isBordered = true, hasBg = true) {
        child(projectInfo) {
            attrs {
                project = state.project
                isEditDisabled = state.isEditDisabled
            }
        }

        div("ml-3 mt-2 align-items-left justify-content-between") {
            fontAwesomeIcon(icon = faHistory)

            button(classes = "btn btn-link text-left") {
                +"Latest Execution"
                attrs.onClickFunction = {
                    scope.launch {
                        switchToLatestExecution(state.latestExecutionId)
                    }
                }
                attrs.disabled = state.latestExecutionId == null
            }
        }
        div("ml-3 align-items-left") {
            fontAwesomeIcon(icon = faCalendarAlt)
            a(
                href = "#/${state.project.organization.name}/${state.project.name}/history",
                classes = "btn btn-link text-left"
            ) {
                +"Execution History"
            }
        }
    }
    private val typeSelection = cardComponent {
        div("text-left") {
            testingTypeButton(
                TestingType.CUSTOM_TESTS,
                "Evaluate your tool with your own tests from git",
                "mr-2"
            )
            testingTypeButton(
                TestingType.STANDARD_BENCHMARKS,
                "Evaluate your tool with standard test suites",
                "mt-3 mr-2"
            )
            testingTypeButton(
                TestingType.CONTEST_MODE,
                "Participate in SAVE contests with your tool",
                "mt-3 mr-2"
            )
        }
    }
    private val fileUploader = fileUploader(
        onFileSelect = { element ->
            setState {
                val availableFile = availableFiles.first { it.name == element.value }
                files.add(availableFile)
                bytesReceived += availableFile.sizeBytes
                suiteByteSize += availableFile.sizeBytes
                availableFiles.remove(availableFile)
            }
        },
        onFileRemove = {
            setState {
                files.remove(it)
                bytesReceived -= it.sizeBytes
                suiteByteSize -= it.sizeBytes
                availableFiles.add(it)
            }
        },
        onFileInput = { postFileUpload(it) },
        onExecutableChange = { selectedFile, checked ->
            setState {
                files[files.indexOf(selectedFile)] = selectedFile.copy(isExecutable = checked)
            }
        }
    )
    private val sdkSelection = sdkSelection({
        setState {
            selectedSdk = it.value
            selectedSdkVersion = selectedSdk.getSdkVersions().first()
        }
    }, {
        setState { selectedSdkVersion = it.value }
    })
    private lateinit var responseFromDeleteProject: Response

    init {
        state.project = Project("N/A", "N/A", "N/A", ProjectStatus.CREATED, userId = -1, organization = Organization("stub", null, date))
        state.gitUrlFromInputField = ""
        state.gitBranchOrCommitFromInputField = ""
        state.execCmd = ""
        state.batchSizeForAnalyzer = ""
        state.testRootPath = ""
        state.confirmationType = ConfirmationType.NO_CONFIRM
        state.testingType = TestingType.CUSTOM_TESTS
        state.isErrorOpen = false
        state.isSubmitButtonPressed = false
        state.errorMessage = ""
        state.errorLabel = ""
        state.isLoading = true
        state.files = mutableListOf()
        state.availableFiles = mutableListOf()
        state.selectedSdk = Sdk.Default.name
        state.selectedSdkVersion = Sdk.Default.version
        state.selectedLanguageForStandardTests = ""
        state.suiteByteSize = state.files.sumOf { it.sizeBytes }
        state.bytesReceived = state.availableFiles.sumOf { it.sizeBytes }
        state.isUploading = false
        state.isEditDisabled = true
        state.selectedMenu = ProjectMenuBar.RUN
        state.isOpenMenuStatistic = false
        state.isOpenMenuSettings = false
    }

    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            val result = getProject(props.name, props.owner)
            val project = if (result.isFailure) {
                setState { isLoading = false }
                return@launch
            } else {
                result.getOrThrow()
            }
            setState {
                this.project = project
            }

            val jsonProject = Json.encodeToString(project)
            val headers = Headers().apply {
                set("Accept", "application/json")
                set("Content-Type", "application/json")
            }
            gitDto = post("$apiUrl/projects/git", headers, jsonProject)
                .decodeFromJsonString<GitDto>()
            standardTestSuites = get(
                "$apiUrl/allStandardTestSuites",
                headers,
                responseHandler = ::noopResponseHandler,
            )
                .decodeFromJsonString()

            val availableFiles = getFilesList()
            setState {
                this.availableFiles.clear()
                this.availableFiles.addAll(availableFiles)
                isLoading = false
            }

            fetchLatestExecutionId()
        }
    }

    @Suppress("ComplexMethod", "TOO_LONG_FUNCTION")
    private fun submitExecutionRequest() {
        when (state.testingType) {
            TestingType.CUSTOM_TESTS -> {
                val urlWithTests = state.gitUrlFromInputField
                val branchOrCommit = state.gitBranchOrCommitFromInputField
                // URL is required in all cases, the processing should not be done without it
                if (urlWithTests.isBlank()) {
                    return
                } else {
                    // if provided value contains `origin` then it's a branch, otherwise a commit
                    val (newBranch, newCommit) = if (branchOrCommit.contains("origin/")) {
                        branchOrCommit to null
                    } else {
                        null to branchOrCommit
                    }
                    val newGitDto = gitDto?.copy(url = urlWithTests, branch = newBranch, hash = newCommit) ?: GitDto(url = urlWithTests, branch = newBranch, hash = newCommit)
                    submitExecutionRequestWithCustomTests(newGitDto)
                }
            }
            else -> {
                if (selectedStandardSuites.isEmpty()) {
                    setState {
                        isErrorOpen = true
                        errorLabel = "Both type of project"
                        errorMessage = "Please choose at least one test suite"
                    }
                    return
                }
                submitExecutionRequestWithStandardTests()
            }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    private fun submitExecutionRequestWithStandardTests() {
        val headers = Headers()
        val formData = FormData()
        val selectedSdk = "${state.selectedSdk}:${state.selectedSdkVersion}".toSdk()
        val request = ExecutionRequestForStandardSuites(state.project, selectedStandardSuites, selectedSdk, state.execCmd, state.batchSizeForAnalyzer)
        formData.appendJson("execution", request)
        state.files.forEach {
            formData.appendJson("file", it)
        }
        submitRequest("/executionRequestStandardTests", headers, formData)
    }

    private fun submitExecutionRequestWithCustomTests(correctGitDto: GitDto) {
        val selectedSdk = "${state.selectedSdk}:${state.selectedSdkVersion}".toSdk()
        val formData = FormData()
        val testRootPath = state.testRootPath.ifBlank { "." }
        val executionRequest = ExecutionRequest(state.project, correctGitDto, testRootPath, selectedSdk, null)
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
        scope.launch {
            val response = post(apiUrl + url, headers, body)
            if (!response.ok) {
                response.text().then { text ->
                    setState {
                        isErrorOpen = true
                        errorLabel = "Error from backend"
                        errorMessage = "Request failed: [${response.statusText}] $text"
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
        runConfirmWindowModal(
            state.isConfirmWindowOpen,
            state.confirmLabel,
            state.confirmMessage,
            { setState { isConfirmWindowOpen = false } }) {
            when (state.confirmationType) {
                ConfirmationType.NO_BINARY_CONFIRM, ConfirmationType.NO_CONFIRM -> submitExecutionRequest()
                ConfirmationType.DELETE_CONFIRM -> deleteProjectBuilder()
                else -> {
                    // this is a generated else block
                }
            }
            setState { isConfirmWindowOpen = false }
        }
        runLoadingModal()
        // Page Heading
        div("d-sm-flex align-items-center justify-content-center mb-4") {
            h1("h3 mb-0 text-gray-800") {
                +" Project ${state.project.name}"
            }
            privacySpan(state.project)
        }

        div("row align-items-center justify-content-center") {
            nav("nav nav-tabs mb-4") {
                ProjectMenuBar.values().forEachIndexed { i, projectMenu ->
                    li("nav-item") {
                        val classVal = if ((i == 0 && state.selectedMenu == null) || state.selectedMenu == projectMenu) " active font-weight-bold" else ""
                        p("nav-link $classVal text-gray-800") {
                            attrs.onClickFunction = {
                                if (state.selectedMenu != projectMenu) {
                                    setState {
                                        selectedMenu = projectMenu
                                    }
                                }
                                if (projectMenu != ProjectMenuBar.STATISTICS) {
                                    openMenuStatisticFlag(false)
                                }
                                if (projectMenu != ProjectMenuBar.SETTINGS) {
                                    openMenuSettingsFlag(false)
                                }
                            }
                            +projectMenu.name
                        }
                    }
                }
            }
        }

        if (state.selectedMenu == ProjectMenuBar.RUN) {
            div("row justify-content-center ml-5") {
                // ===================== LEFT COLUMN =======================================================================
                div("col-2 mr-3") {
                    div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                        +"Testing types"
                    }

                    child(typeSelection)
                }
                // ===================== MIDDLE COLUMN =====================================================================
                div("col-4") {
                    div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                        +"Test configuration"
                    }

                    // ======== file selector =========
                    child(fileUploader) {
                        attrs.isSubmitButtonPressed = state.isSubmitButtonPressed
                        attrs.files = state.files
                        attrs.availableFiles = state.availableFiles
                        attrs.confirmationType = state.confirmationType
                        attrs.suiteByteSize = state.suiteByteSize
                        attrs.bytesReceived = state.bytesReceived
                        attrs.isUploading = state.isUploading
                    }

                    // ======== sdk selection =========
                    child(sdkSelection) {
                        attrs.selectedSdk = state.selectedSdk
                        attrs.selectedSdkVersion = state.selectedSdkVersion
                    }

                    // ======== test resources selection =========
                    child(testResourcesSelection) {
                        attrs.testingType = state.testingType
                        attrs.isSubmitButtonPressed = state.isSubmitButtonPressed
                        attrs.gitDto = gitDto
                        // properties for CUSTOM_TESTS mode
                        attrs.testRootPath = state.testRootPath
                        attrs.gitUrlFromInputField = state.gitUrlFromInputField
                        attrs.gitBranchOrCommitFromInputField = state.gitBranchOrCommitFromInputField
                        // properties for STANDARD_BENCHMARKS mode
                        attrs.selectedStandardSuites = selectedStandardSuites
                        attrs.standardTestSuites = standardTestSuites
                        attrs.selectedLanguageForStandardTests = state.selectedLanguageForStandardTests
                        attrs.execCmd = state.execCmd
                        attrs.batchSizeForAnalyzer = state.batchSizeForAnalyzer
                    }

                    div("d-sm-flex align-items-center justify-content-center") {
                        button(type = ButtonType.button, classes = "btn btn-primary") {
                            attrs.onClickFunction = { submitWithValidation() }
                            +"Test the tool now"
                        }
                    }
                }
                // ===================== RIGHT COLUMN ======================================================================
                div("col-3 ml-2") {
                    div("text-xs text-center font-weight-bold text-primary text-uppercase mb-3") {
                        +"Information"
                        button(classes = "btn btn-link text-xs text-muted text-left p-1 ml-2") {
                            +"Edit  "
                            fontAwesomeIcon(icon = faEdit)
                            attrs.onClickFunction = {
                                turnEditMode(isOff = false)
                            }
                        }
                    }

                    child(projectInfoCard)
                }
            }
        } else if (state.selectedMenu == ProjectMenuBar.STATISTICS) {
            child(projectStatisticMenu) {
                attrs.executionId = state.latestExecutionId
                attrs.isOpen = state.isOpenMenuStatistic
            }
        } else if (state.selectedMenu == ProjectMenuBar.SETTINGS) {
            child(projectSettingsMenu) {
                attrs.isOpen = state.isOpenMenuSettings
                attrs.project = state.project
                attrs.projectUsers = emptyList()
                attrs.selfRole = Role.VIEWER
                attrs.currentUserInfo = props.currentUserInfo ?: UserInfo("Unknown")
            }
        }
    }

    private fun postFileUpload(element: HTMLInputElement) =
            scope.launch {
                setState {
                    isUploading = true
                    element.files!!.asList().forEach { file ->
                        suiteByteSize += file.size.toLong()
                    }
                }

                element.files!!.asList().forEach { file ->
                    val response: FileInfo = post(
                        "$apiUrl/files/upload",
                        Headers(),
                        FormData().apply {
                            append("file", file)
                        }
                    )
                        .decodeFromJsonString()
                    setState {
                        // add only to selected files so that this entry isn't duplicated
                        files.add(response)
                        bytesReceived += response.sizeBytes
                    }
                }
                setState {
                    isUploading = false
                }
            }

    private fun openMenuStatisticFlag(isOpen: Boolean) {
        setState {
            isOpenMenuStatistic = isOpen
        }
    }

    private fun openMenuSettingsFlag(isOpen: Boolean) {
        setState {
            isOpenMenuSettings = isOpen
        }
    }

    private fun turnEditMode(isOff: Boolean) {
        setState {
            isEditDisabled = isOff
        }
        (document.getElementById("Save new project info") as HTMLButtonElement).hidden = isOff
        (document.getElementById("Cancel") as HTMLButtonElement).hidden = isOff
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

    private fun RBuilder.testingTypeButton(selectedTestingType: TestingType, text: String, divClass: String) {
        div(divClass) {
            button(type = ButtonType.button) {
                attrs.classes =
                        if (state.testingType == selectedTestingType) {
                            setOf("btn", "btn-primary")
                        } else {
                            setOf(
                                "btn",
                                "btn-outline-primary"
                            )
                        }
                attrs.onClickFunction = {
                    setState {
                        testingType = selectedTestingType
                    }
                }
                +text
            }
        }
    }

    /**
     * In some cases scripts and binaries can be uploaded to a git repository, so users won't be providing or uploading
     * binaries. For this case we should open a window, so user will need to click a checkbox, so he will confirm that
     * he understand what he is doing.
     */
    private fun submitWithValidation() {
        setState {
            isSubmitButtonPressed = true
        }
        when {
            // url was not provided
            state.gitUrlFromInputField.isBlank() && state.testingType == TestingType.CUSTOM_TESTS -> setState {
                isErrorOpen = true
                errorMessage =
                        "Git Url with test suites in save format was not provided,but it is required for the testing process." +
                                " SAVE is not able to run your tests without an information of where to download them from."
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
        val newProject = state.project.copy(status = ProjectStatus.DELETED)

        setState {
            project = newProject
            confirmationType = ConfirmationType.DELETE_CONFIRM
            isConfirmWindowOpen = true
            confirmLabel = ""
            confirmMessage = "Are you sure you want to delete this project?"
        }
    }

    @Suppress("COMMENTED_OUT_CODE")
    private suspend fun updateProject(draftProject: Project): Response {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        return post("$apiUrl/projects/update", headers, Json.encodeToString(draftProject))
    }

    private fun deleteProjectBuilder() {
        val headers = Headers().also {
            it.set("Accept", "application/json")
            it.set("Content-Type", "application/json")
        }
        scope.launch {
            responseFromDeleteProject =
                    post("$apiUrl/projects/update", headers, Json.encodeToString(state.project))
        }.invokeOnCompletion {
            if (responseFromDeleteProject.ok) {
                window.location.href = "${window.location.origin}/"
            } else {
                responseFromDeleteProject.text().then {
                    setState {
                        errorLabel = "Failed to delete project"
                        errorMessage = it
                        isErrorOpen = true
                    }
                }
            }
        }
    }

    private suspend fun fetchLatestExecutionId() {
        val headers = Headers().apply { set("Accept", "application/json") }
        val response = get(
            "$apiUrl/latestExecution?name=${state.project.name}&organizationId=${state.project.organization.id}",
            headers,
            responseHandler = ::noopResponseHandler
        )
        when {
            !response.ok -> setState {
                errorLabel = "Failed to fetch latest execution"
                errorMessage =
                        "Failed to fetch latest execution: [${response.status}] ${response.statusText}, please refresh the page and try again"
                latestExecutionId = null
            }
            response.status == 204.toShort() -> setState {
                latestExecutionId = null
            }
            else -> {
                val latestExecutionIdNew = response
                    .decodeFromJsonString<ExecutionDto>()
                    .id
                setState {
                    latestExecutionId = latestExecutionIdNew
                }
            }
        }
    }

    private fun switchToLatestExecution(latestExecutionId: Long?) {
        latestExecutionId?.let {
            window.location.href = "${window.location}/history/execution/$latestExecutionId"
        }
            ?: setState {
                isErrorOpen = true
            }
    }

    private suspend fun getFilesList() = get("$apiUrl/files/list", Headers())
        .unsafeMap {
            it.decodeFromJsonString<List<FileInfo>>()
        }

    companion object : RStatics<ProjectExecutionRouteProps, ProjectViewState, ProjectView, Context<StateSetter<Response?>>>(ProjectView::class) {
        const val TEST_ROOT_DIR_HINT = """
            The path you are providing should be relative to the root directory of your repository.
            This directory should contain <a href = "https://github.com/analysis-dev/save#how-to-configure"> save.properties </a>
            or <a href = "https://github.com/analysis-dev/save#-savetoml-configuration-file">save.toml</a> files.
            For example, if the URL to your repo with tests is: 
            <a href ="https://github.com/analysis-dev/save/">https://github.com/analysis-dev/save</a>, then
            you need to specify the following directory with 'save.toml': 
            <a href ="https://github.com/analysis-dev/save/tree/main/examples/kotlin-diktat">examples/kotlin-diktat/</a>.
 
            Please note, that the tested tool and it's resources will be copied to this directory before the run.
            """
        const val TEST_SUITE_ROW = 4
        init {
            contextType = errorStatusContext
        }
    }
}
