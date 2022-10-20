/**
 * A view with project details
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.views

import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.*
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.TestingType
import com.saveourtool.save.frontend.components.RequestStatusContext
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.projects.projectInfoMenu
import com.saveourtool.save.frontend.components.basic.projects.projectSettingsMenu
import com.saveourtool.save.frontend.components.basic.projects.projectStatisticMenu
import com.saveourtool.save.frontend.components.modal.displayModal
import com.saveourtool.save.frontend.components.modal.mediumTransparentModalStyle
import com.saveourtool.save.frontend.components.requestStatusContext
import com.saveourtool.save.frontend.externals.fontawesome.faCalendarAlt
import com.saveourtool.save.frontend.externals.fontawesome.faHistory
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.http.getProject
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.HasSelectedMenu
import com.saveourtool.save.frontend.utils.changeUrl
import com.saveourtool.save.frontend.utils.noopResponseHandler
import com.saveourtool.save.frontend.utils.urlAnalysis
import com.saveourtool.save.info.UserInfo
import com.saveourtool.save.request.CreateExecutionRequest
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.getHighestRole

import csstype.ClassName
import history.Location
import org.w3c.fetch.Headers
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p

import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * [Props] retrieved from router
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface ProjectViewProps : PropsWithChildren {
    var owner: String
    var name: String
    var currentUserInfo: UserInfo?
    var location: Location
}

/**
 * [State] of project view component for CONTEST run
 */
external interface ContestRunState : State {
    /**
     * Currently selected contest
     */
    var selectedContest: ContestDto

    /**
     * All available contest
     */
    var availableContests: List<ContestDto>

    /**
     * All available contest
     */
    var selectedContestTestSuites: List<TestSuiteDto>
}

/**
 * [State] of project view component
 */
external interface ProjectViewState : StateWithRole, ContestRunState, HasSelectedMenu<ProjectMenuBar> {
    /**
     * Currently loaded for display Project
     */
    var project: Project

    /**
     * Files required for tests execution for this project
     */
    var files: List<FileInfo>

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
     * Selected sdk
     */
    var selectedSdk: Sdk

    /**
     * Flag to handle upload type project
     */
    var testingType: TestingType

    /**
     * List of Test Suites of private [TestSuiteDto] for execution run
     */
    var selectedPrivateTestSuites: List<TestSuiteDto>

    /**
     * List of Test Suites of public [TestSuiteDto] for execution run
     */
    var selectedPublicTestSuites: List<TestSuiteDto>

    /**
     * Execution command for standard mode
     */
    var execCmd: String

    /**
     * Batch size for static analyzer tool in standard mode
     */
    var batchSizeForAnalyzer: String

    /**
     * latest execution id for this project
     */
    var latestExecutionId: Long?

    /**
     * Label that will be shown on close button
     */
    var closeButtonLabel: String?

    /**
     * Contains the paths of default and other tabs
     */
    var paths: PathsForTabs
}

/**
 * A Component for project view
 * Each modal opening call causes re-render of the whole page, that's why we need to use state for all fields
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
@Suppress("MAGIC_NUMBER")
class ProjectView : AbstractView<ProjectViewProps, ProjectViewState>(false) {
    private val projectInfoCard = cardComponent(isBordered = true, hasBg = true)
    private val typeSelection = cardComponent()

    init {
        state.project = Project.stub(null, Organization.stub(null))
        state.selectedContest = ContestDto.empty
        state.availableContests = emptyList()
        state.selectedPrivateTestSuites = emptyList()
        state.selectedPublicTestSuites = emptyList()
        state.execCmd = ""
        state.batchSizeForAnalyzer = ""
        state.testingType = TestingType.PRIVATE_TESTS
        state.selectedContest = ContestDto.empty
        state.availableContests = emptyList()
        state.isErrorOpen = false
        state.errorMessage = ""
        state.errorLabel = ""
        state.files = mutableListOf()
        state.selectedSdk = Sdk.Default
        state.selectedMenu = ProjectMenuBar.defaultTab
        state.closeButtonLabel = null
        state.selfRole = Role.NONE
        state.selectedContestTestSuites = emptyList()
    }

    private fun showNotification(notificationLabel: String, notificationMessage: String) {
        setState {
            isErrorOpen = true
            errorLabel = notificationLabel
            errorMessage = notificationMessage
            closeButtonLabel = "Confirm"
        }
    }

    override fun componentDidUpdate(prevProps: ProjectViewProps, prevState: ProjectViewState, snapshot: Any) {
        if (prevState.selectedMenu != state.selectedMenu) {
            changeUrl(state.selectedMenu, ProjectMenuBar, state.paths)
        } else if (props.location != prevProps.location) {
            urlAnalysis(ProjectMenuBar, state.selfRole, false)
        }
        if (prevState.selectedContestTestSuites != state.selectedContestTestSuites) {
            fetchTestSuiteDtos(state.selectedContest.testSuiteIds)
        }
    }

    @Suppress("TOO_LONG_FUNCTION")
    override fun componentDidMount() {
        super.componentDidMount()

        scope.launch {
            val result = getProject(props.name, props.owner)
            val project = if (result.isFailure) {
                return@launch
            } else {
                result.getOrThrow()
            }
            setState {
                this.project = project
                paths = PathsForTabs("/${props.owner}/${props.name}", "#/${ProjectMenuBar.nameOfTheHeadUrlSection}/${props.owner}/${props.name}")
            }
            val currentUserRoleInProject: Role = get(
                "$apiUrl/projects/${project.organization.name}/${project.name}/users/roles",
                jsonHeaders,
                loadingHandler = ::classLoadingHandler,
            ).decodeFromJsonString()

            val currentUserRoleInOrganization: Role = get(
                url = "$apiUrl/organizations/${project.organization.name}/users/roles",
                headers = jsonHeaders,
                loadingHandler = ::classLoadingHandler,
            ).decodeFromJsonString()

            val currentUserRole = getHighestRole(currentUserRoleInProject, currentUserRoleInOrganization)

            val role = getHighestRole(currentUserRole, props.currentUserInfo?.globalRole)
            setState {
                selfRole = role
            }

            urlAnalysis(ProjectMenuBar, role, false)

            val contests = getContests()
            setState {
                availableContests = contests
                contests.firstOrNull()?.let { selectedContest = it }
            }

            fetchLatestExecutionId()
            fetchTestSuiteDtos(state.selectedContest.testSuiteIds)
        }
    }

    @Suppress("ComplexMethod", "TOO_LONG_FUNCTION")
    private fun NavigateFunctionContext.submitExecutionRequest() {
        when (state.testingType) {
            TestingType.PRIVATE_TESTS -> submitExecutionRequestByTestSuiteIds(state.selectedPrivateTestSuites, state.testingType)
            TestingType.PUBLIC_TESTS -> submitExecutionRequestByTestSuiteIds(state.selectedPublicTestSuites, state.testingType)
            TestingType.CONTEST_MODE -> submitExecutionRequestByTestSuiteIds(state.selectedContestTestSuites, state.testingType)
            else -> throw IllegalStateException("Not supported testing type: ${state.testingType}")
        }
    }

    private fun NavigateFunctionContext.submitExecutionRequestByTestSuiteIds(selectedTestSuites: List<TestSuiteDto>, testingType: TestingType) {
        val executionRequest = CreateExecutionRequest(
            projectCoordinates = ProjectCoordinates(
                organizationName = state.project.organization.name,
                projectName = state.project.name
            ),
            testSuiteIds = selectedTestSuites.map { it.requiredId() },
            files = state.files.map { it.key },
            sdk = state.selectedSdk,
            execCmd = state.execCmd.takeUnless { it.isBlank() },
            batchSizeForAnalyzer = state.batchSizeForAnalyzer.takeUnless { it.isBlank() },
            testingType = testingType,
            contestName = testingType.takeIf { it == TestingType.CONTEST_MODE }?.let { state.selectedContest.name }
        )
        submitRequest("/run/trigger", jsonHeaders, Json.encodeToString(executionRequest))
    }

    private fun NavigateFunctionContext.submitRequest(url: String, headers: Headers, body: dynamic) {
        scope.launch {
            val response = post(
                apiUrl + url,
                headers,
                body,
                loadingHandler = ::classLoadingHandler,
            )
            if (response.ok) {
                navigate(to = "/${state.project.organization.name}/${state.project.name}/history")
            }
        }
    }

    // fixme: can be removed after https://github.com/saveourtool/save-cloud/issues/1192
    private fun fetchTestSuiteDtos(ids: List<Long>) {
        scope.launch {
            val testSuitesFromBackend: List<TestSuiteDto> = post(
                url = "$apiUrl/test-suites/${props.owner}/get-by-ids",
                headers = jsonHeaders,
                body = Json.encodeToString(ids),
                loadingHandler = ::classLoadingHandler,
                responseHandler = ::noopResponseHandler,
            )
                .decodeFromJsonString()
            setState {
                selectedContestTestSuites = testSuitesFromBackend
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "ComplexMethod")
    override fun ChildrenBuilder.render() {
        val modalCloseCallback = {
            setState {
                isErrorOpen = false
                closeButtonLabel = null
            }
        }
        displayModal(
            state.isErrorOpen,
            state.errorLabel,
            state.errorMessage,
            mediumTransparentModalStyle,
            modalCloseCallback,
        ) {
            buttonBuilder(state.closeButtonLabel ?: "Close", "secondary") {
                modalCloseCallback()
            }
        }
        // Page Heading
        div {
            className = ClassName("d-sm-flex align-items-center justify-content-center mb-4")
            h1 {
                className = ClassName("h3 mb-0 text-gray-800")
                +" Project ${state.project.name}"
            }
            privacySpan(state.project)
        }

        renderProjectMenuBar()

        when (state.selectedMenu) {
            ProjectMenuBar.RUN -> renderRun()
            ProjectMenuBar.STATISTICS -> renderStatistics()
            ProjectMenuBar.SETTINGS -> renderSettings()
            ProjectMenuBar.INFO -> renderInfo()
        }
    }

    private fun ChildrenBuilder.renderProjectMenuBar() {
        div {
            className = ClassName("row align-items-center justify-content-center")
            nav {
                className = ClassName("nav nav-tabs mb-4")
                ProjectMenuBar.values()
                    .filterNot {
                        (it == ProjectMenuBar.RUN || it == ProjectMenuBar.SETTINGS) && !state.selfRole.isHigherOrEqualThan(Role.ADMIN)
                    }
                    .forEach { projectMenu ->
                        li {
                            className = ClassName("nav-item")
                            val classVal = if (state.selectedMenu == projectMenu) " active font-weight-bold" else ""
                            p {
                                className = ClassName("nav-link $classVal text-gray-800")
                                onClick = {
                                    if (state.selectedMenu != projectMenu) {
                                        setState { selectedMenu = projectMenu }
                                    }
                                }
                                +projectMenu.name
                            }
                        }
                    }
            }
        }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    private fun ChildrenBuilder.renderRun() {
        div {
            className = ClassName("row justify-content-center ml-5")
            // ===================== LEFT COLUMN =======================================================================
            div {
                className = ClassName("col-2 mr-3")
                div {
                    className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Testing types"
                }

                typeSelection {
                    div {
                        className = ClassName("text-left")
                        testingTypeButton(
                            TestingType.PRIVATE_TESTS,
                            "Evaluate your tool with your own tests",
                            "mr-2"
                        )
                        testingTypeButton(
                            TestingType.PUBLIC_TESTS,
                            "Evaluate your tool with public test suites",
                            "mt-3 mr-2"
                        )
                        if (state.project.public) {
                            testingTypeButton(
                                TestingType.CONTEST_MODE,
                                "Participate in SAVE contests with your tool",
                                "mt-3 mr-2"
                            )
                        }
                    }
                }
            }
            // ===================== MIDDLE COLUMN =====================================================================
            div {
                className = ClassName("col-4")
                div {
                    className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Test configuration"
                }

                // ======== file selector =========
                div {
                    label {
                        className =
                                ClassName("control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-1 pl-0")
                        +"1. Upload or select the tool (and other resources) for testing:"
                    }
                    fileUploaderForProjectRun(
                        ProjectCoordinates(props.owner, props.name),
                        state.files,
                        { fileToAdd ->
                            setState {
                                files = files.toMutableList().apply {
                                    add(fileToAdd)
                                }.toList()
                            }
                        }
                    ) { fileToRemove ->
                        setState {
                            files = files.toMutableList().apply {
                                remove(fileToRemove)
                            }
                        }
                    }
                }

                // ======== sdk selection =========
                sdkSelection {
                    title = "2. Select the SDK if needed:"
                    selectedSdk = state.selectedSdk
                    onSdkChange = {
                        setState {
                            selectedSdk = it
                        }
                    }
                }

                // ======== test resources selection =========
                testResourcesSelection {
                    testingType = state.testingType
                    // properties for CONTEST_TESTS mode
                    projectName = props.name
                    organizationName = props.owner
                    onContestEnrollerResponse = {
                        setState {
                            isErrorOpen = true
                            errorMessage = it
                            errorLabel = "Contest enrollment"
                        }
                    }
                    selectedContest = state.selectedContest
                    setSelectedContest = { selectedContest ->
                        setState {
                            this.selectedContest = selectedContest
                        }
                    }
                    availableContests = state.availableContests
                    // properties for PRIVATE_TESTS mode
                    selectedPrivateTestSuiteDtos = state.selectedPrivateTestSuites
                    setSelectedPrivateTestSuiteDtos = { selectedTestSuiteDtos ->
                        setState {
                            this.selectedPrivateTestSuites = selectedTestSuiteDtos
                        }
                    }
                    // properties for PUBLIC_TESTS mode
                    selectedPublicTestSuiteDtos = state.selectedPublicTestSuites
                    setSelectedPublicTestSuiteDtos = { selectedTestSuiteDtos ->
                        setState {
                            this.selectedPublicTestSuites = selectedTestSuiteDtos
                        }
                    }
                    // properties for PRIVATE_TESTS and PUBLIC_TESTS modes
                    execCmd = state.execCmd
                    setExecCmd = { execCmd ->
                        setState {
                            this.execCmd = execCmd
                        }
                    }
                    batchSizeForAnalyzer = state.batchSizeForAnalyzer
                    setBatchSizeForAnalyzer = { batchSizeForAnalyzer ->
                        setState {
                            this.batchSizeForAnalyzer = batchSizeForAnalyzer
                        }
                    }
                }

                div {
                    className = ClassName("d-sm-flex align-items-center justify-content-center")
                    withNavigate { navigateContext ->
                        button {
                            type = ButtonType.button
                            disabled = state.files.isEmpty()
                            className = ClassName("btn btn-primary")
                            onClick = { navigateContext.submitExecutionRequest() }
                            +"Test the tool now"
                        }
                    }
                }
            }
            // ===================== RIGHT COLUMN ======================================================================
            div {
                className = ClassName("col-3 ml-2")
                div {
                    className = ClassName("text-xs text-center font-weight-bold text-primary text-uppercase mb-3")
                    +"Information"
                }

                projectInfoCard {
                    projectInfo {
                        project = state.project
                        onProjectUpdate = {
                            setState {
                                project = it
                            }
                        }
                    }

                    div {
                        className = ClassName("ml-3 mt-2 align-items-left justify-content-between")
                        fontAwesomeIcon(icon = faHistory)
                        withNavigate { navigateContext ->
                            button {
                                type = ButtonType.button
                                className = ClassName("btn btn-link text-left")
                                +"Latest Execution"
                                disabled = state.latestExecutionId == null
                                onClick = {
                                    navigateContext.navigateToLinkWithSuffix(state.paths.pathDefaultTab, "history/execution/${state.latestExecutionId}")
                                }
                            }
                        }
                    }
                    div {
                        className = ClassName("ml-3 align-items-left")
                        fontAwesomeIcon(icon = faCalendarAlt)
                        a {
                            href = "#${state.paths.pathDefaultTab}/history"
                            className = ClassName("btn btn-link text-left")
                            +"Execution History"
                        }
                    }
                }
            }
        }
    }

    private fun ChildrenBuilder.renderStatistics() {
        projectStatisticMenu {
            executionId = state.latestExecutionId
        }
    }

    private fun ChildrenBuilder.renderInfo() {
        projectInfoMenu {
            projectName = props.name
            organizationName = props.owner
            latestExecutionId = state.latestExecutionId
        }
    }

    private fun ChildrenBuilder.renderSettings() {
        projectSettingsMenu {
            project = state.project
            currentUserInfo = props.currentUserInfo ?: UserInfo("Unknown")
            selfRole = state.selfRole
            updateErrorMessage = { response, message ->
                setState {
                    errorLabel = response.statusText
                    errorMessage = message
                    isErrorOpen = true
                }
            }
            updateNotificationMessage = ::showNotification
        }
    }

    private fun ChildrenBuilder.testingTypeButton(selectedTestingType: TestingType, text: String, divClass: String) {
        div {
            className = ClassName(divClass)
            button {
                type = ButtonType.button
                className =
                        if (state.testingType == selectedTestingType) {
                            ClassName("btn btn-primary")
                        } else {
                            ClassName("btn btn-outline-primary")
                        }
                onClick = {
                    setState {
                        testingType = selectedTestingType
                    }
                }
                +text
            }
        }
    }

    private suspend fun fetchLatestExecutionId() {
        val response = get(
            "$apiUrl/latestExecution?name=${state.project.name}&organizationName=${state.project.organization.name}",
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
            responseHandler = ::noopResponseHandler,
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
                val executionIdFromResponse: Long = response
                    .decodeFromJsonString<ExecutionDto>().id

                setState {
                    latestExecutionId = executionIdFromResponse
                }
            }
        }
    }

    private suspend fun getContests() = get(
        "$apiUrl/contests/active",
        jsonHeaders,
        loadingHandler = ::noopLoadingHandler,
    )
        .unsafeMap {
            it.decodeFromJsonString<List<ContestDto>>()
        }

    companion object :
        RStatics<ProjectViewProps, ProjectViewState, ProjectView, Context<RequestStatusContext>>(ProjectView::class) {
        const val TEST_ROOT_DIR_HINT = """
            The path you are providing should be relative to the root directory of your repository.
            This directory should contain <a href = "https://github.com/saveourtool/save#how-to-configure"> save.properties </a>
            or <a href = "https://github.com/saveourtool/save-cli#-savetoml-configuration-file">save.toml</a> files.
            For example, if the URL to your repo with tests is: 
            <a href ="https://github.com/saveourtool/save-cli/">https://github.com/saveourtool/save</a>, then
            you need to specify the following directory with 'save.toml': 
            <a href ="https://github.com/saveourtool/save-cli/tree/main/examples/kotlin-diktat">examples/kotlin-diktat/</a>.
 
            Please note, that the tested tool and it's resources will be copied to this directory before the run.
            """

        init {
            contextType = requestStatusContext
        }
    }
}
