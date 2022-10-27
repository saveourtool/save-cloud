@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.basic.projects

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.TestingType
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.externals.fontawesome.faCalendarAlt
import com.saveourtool.save.frontend.externals.fontawesome.faHistory
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.request.CreateExecutionRequest
import com.saveourtool.save.testsuite.TestSuiteDto

import csstype.ClassName
import react.*
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label

import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import react.dom.html.ReactHTML
import react.router.dom.Link

private val typeSelection = cardComponent()

private val projectInfoCard = cardComponent(isBordered = true, hasBg = true)

/**
 * RUN tab in ProjectView
 */
val projectRunMenu = projectRunMenu()

/**
 * ProjectRunMenu component props
 */
external interface ProjectRunMenuProps : Props {
    /**
     * Project name
     */
    var projectName: String

    /**
     * Organization name
     */
    var organizationName: String

    /**
     * ID of the latest execution
     */
    var latestExecutionId: Long?

    /**
     * Path to the default tab of this project's view
     */
    var pathToView: String

    /**
     * Callback to send execution request
     */
    @Suppress("TYPE_ALIAS")
    var submitExecutionRequest: (NavigateFunctionContext, CreateExecutionRequest) -> Unit
}

private fun ChildrenBuilder.testingTypeButton(
    selectedTestingType: TestingType,
    text: String,
    divClass: String,
    testingType: TestingType,
    setTestingType: (TestingType) -> Unit,
) {
    div {
        className = ClassName(divClass)
        buttonBuilder(text, isOutline = true, isActive = testingType == selectedTestingType) {
            setTestingType(selectedTestingType)
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun projectRunMenu() = FC<ProjectRunMenuProps> { props ->
    val (project, setProject) = useState(Project.stub(-1))
    useRequest {
        val projectFromBackend: Project = get(
            url = "$apiUrl/projects/get/organization-name?name=${props.projectName}&organizationName=${props.organizationName}",
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setProject(projectFromBackend)
    }

    val (selectedContest, setSelectedContest) = useState(ContestDto.empty)
    val (availableContests, setAvailableContests) = useState<List<ContestDto>>(emptyList())
    useRequest {
        val contests: List<ContestDto> = get(
            "$apiUrl/contests/active",
            jsonHeaders,
            loadingHandler = ::noopLoadingHandler,
        )
            .unsafeMap {
                it.decodeFromJsonString()
            }
        setAvailableContests(contests)
        contests.firstOrNull()?.let {
            setSelectedContest(it)
        }
    }

    val (testingType, setTestingType) = useState(TestingType.PRIVATE_TESTS)
    val (files, setFiles) = useState<List<FileInfo>>(emptyList())

    val (selectedSdk, setSelectedSdk) = useState<Sdk>(Sdk.Default)
    val (execCmd, setExecCmd) = useState("")
    val (batchSizeForAnalyzer, setBatchSizeForAnalyzer) = useState("")

    val (selectedPrivateTestSuites, setSelectedPrivateTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    val (selectedPublicTestSuites, setSelectedPublicTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    val (selectedContestTestSuites, setSelectedContestTestSuites) = useState<List<TestSuiteDto>>(emptyList())
    useRequest(arrayOf(selectedContest)) {
        val testSuitesFromBackend: List<TestSuiteDto> = post(
            url = "$apiUrl/test-suites/${props.organizationName}/get-by-ids",
            headers = jsonHeaders,
            body = Json.encodeToString(selectedContest.testSuiteIds),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .decodeFromJsonString()
        setSelectedContestTestSuites(testSuitesFromBackend)
    }

    val buildExecutionRequest: () -> CreateExecutionRequest = {
        val selectedTestSuites = when (testingType) {
            TestingType.PRIVATE_TESTS -> selectedPrivateTestSuites
            TestingType.PUBLIC_TESTS -> selectedPublicTestSuites
            TestingType.CONTEST_MODE -> selectedContestTestSuites
        }
        CreateExecutionRequest(
            projectCoordinates = ProjectCoordinates(
                organizationName = project.organization.name,
                projectName = project.name
            ),
            testSuiteIds = selectedTestSuites.map { it.requiredId() },
            files = files.map { it.key },
            sdk = selectedSdk,
            execCmd = execCmd.takeUnless { it.isBlank() },
            batchSizeForAnalyzer = batchSizeForAnalyzer.takeUnless { it.isBlank() },
            testingType = testingType,
            contestName = testingType.takeIf { it == TestingType.CONTEST_MODE }?.let { selectedContest.name }
        )
    }

    @Suppress("TYPE_ALIAS")
    val displayTestingTypeButton: ChildrenBuilder.(TestingType, String, String) -> Unit = { selectedTestingType, text, divClass ->
        testingTypeButton(selectedTestingType, text, divClass, testingType) {
            setTestingType(it)
        }
    }

    val isRunButtonDisabled: () -> Boolean = {
        files.isEmpty() || (testingType == TestingType.PRIVATE_TESTS && selectedPrivateTestSuites.isEmpty()) ||
                (testingType == TestingType.PUBLIC_TESTS && selectedPublicTestSuites.isEmpty())
    }

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
                    displayTestingTypeButton(
                        TestingType.PRIVATE_TESTS,
                        "Evaluate your tool with your own tests",
                        "mr-2",
                    )
                    displayTestingTypeButton(
                        TestingType.PUBLIC_TESTS,
                        "Evaluate your tool with public test suites",
                        "mt-3 mr-2",
                    )
                    if (project.public) {
                        displayTestingTypeButton(
                            TestingType.CONTEST_MODE,
                            "Participate in SAVE contests with your tool",
                            "mt-3 mr-2",
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
                className = ClassName("mb-2")
                label {
                    className =
                            ClassName("control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-1 pl-0")
                    +"1. Upload or select the tool (and other resources) for testing:"
                }
                fileUploaderForProjectRun(
                    ProjectCoordinates(props.organizationName, props.projectName),
                    files,
                    { fileToAdd ->
                        setFiles { it + fileToAdd }
                    }
                ) { fileToRemove ->
                    setFiles { it - fileToRemove }
                }
            }

            // ======== sdk selection =========
            sdkSelection {
                title = "2. Select the SDK if needed:"
                this.selectedSdk = selectedSdk
                onSdkChange = { setSelectedSdk(it) }
            }

            // ======== test resources selection =========
            testResourcesSelection {
                this.testingType = testingType
                // properties for CONTEST_TESTS mode
                projectName = props.projectName
                organizationName = props.organizationName
                onContestEnrollerResponse = { window.alert(it) }
                this.selectedContest = selectedContest
                this.setSelectedContest = { setSelectedContest(it) }
                this.availableContests = availableContests
                // properties for PRIVATE_TESTS mode
                selectedPrivateTestSuiteDtos = selectedPrivateTestSuites
                setSelectedPrivateTestSuiteDtos = { setSelectedPrivateTestSuites(it) }
                // properties for PUBLIC_TESTS mode
                selectedPublicTestSuiteDtos = selectedPublicTestSuites
                setSelectedPublicTestSuiteDtos = { setSelectedPublicTestSuites(it) }
                // properties for PRIVATE_TESTS and PUBLIC_TESTS modes
                this.execCmd = execCmd
                this.setExecCmd = { setExecCmd(it) }
                this.batchSizeForAnalyzer = batchSizeForAnalyzer
                this.setBatchSizeForAnalyzer = { setBatchSizeForAnalyzer(batchSizeForAnalyzer) }
            }

            div {
                className = ClassName("d-sm-flex align-items-center justify-content-center")
                withNavigate { navigateContext ->
                    buttonBuilder("Test the tool now", isDisabled = isRunButtonDisabled()) {
                        props.submitExecutionRequest(navigateContext, buildExecutionRequest())
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
                    this.project = project
                    this.onProjectUpdate = { setProject(it) }
                }

                div {
                    className = ClassName("ml-3 mt-2 align-items-left justify-content-between")
                    fontAwesomeIcon(icon = faHistory)
                    withNavigate { navigateContext ->
                        buttonBuilder("Latest Execution", "link", isDisabled = props.latestExecutionId == null, classes = "text-left") {
                            navigateContext.navigateToLinkWithSuffix(props.pathToView, "history/execution/${props.latestExecutionId}")
                        }
                    }
                }
                div {
                    className = ClassName("ml-3 align-items-left")
                    fontAwesomeIcon(icon = faCalendarAlt)
                    withNavigate { navigateContext ->
                        buttonBuilder("Execution History", "link", classes = "text-left") {
                            navigateContext.navigateToLinkWithSuffix(props.pathToView, "history")
                        }
                    }
                }
            }
        }
    }
}
