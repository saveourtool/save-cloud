/**
 * Component for selecting test suites
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.testsuiteselector

import com.saveourtool.save.frontend.components.modal.largeTransparentModalStyle
import com.saveourtool.save.frontend.components.modal.modal
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.externals.modal.*
import com.saveourtool.save.frontend.utils.WindowOpenness
import com.saveourtool.save.frontend.utils.buttonWithIcon
import com.saveourtool.save.frontend.utils.useTooltip
import com.saveourtool.save.testsuite.TestSuiteDto

import csstype.ClassName
import react.*
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5

val testSuiteSelector = testSuiteSelector()

/**
 * [Props] for [testSuiteSelector] component
 */
external interface TestSuiteSelectorProps : Props {
    /**
     * Lambda invoked when test suites were successfully set
     */
    var onTestSuiteUpdate: (List<TestSuiteDto>) -> Unit

    /**
     * List of test suite ids that should be preselected
     */
    var preselectedTestSuites: List<TestSuiteDto>

    /**
     * Specific organization name which reduces list of test suites source.
     * If null, all the test suites are shown
     */
    var specificOrganizationName: String?

    /**
     * Mode that defines what kind of test suites will be shown
     */
    var selectorPurpose: TestSuiteSelectorPurpose

    /**
     * Name of a current organization (by which test suite selection is happening)
     */
    var currentOrganizationName: String
}

/**
 * Enum that represents different modes of [testSuiteSelector]
 */
enum class TestSuiteSelectorMode {
    BROWSER,
    MANAGER,
    SEARCH,
    ;
}

/**
 * Enum that defines what type of test suites should be shown
 */
enum class TestSuiteSelectorPurpose {
    CONTEST,
    PRIVATE,
    PUBLIC,
    ;
}

/**
 * Browse all the available test suites.
 *
 * @param currentOrganizationName
 * @param initTestSuiteDtos initial value
 * @param windowOpenness state to control openness of window
 * @param testSuiteDtosInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuiteIds consumer for result
 */
@Suppress("TYPE_ALIAS")
fun ChildrenBuilder.showPublicTestSuitesSelectorModal(
    currentOrganizationName: String,
    initTestSuiteDtos: List<TestSuiteDto>,
    windowOpenness: WindowOpenness,
    testSuiteDtosInSelectorState: StateInstance<List<TestSuiteDto>>,
    setSelectedTestSuiteIds: (List<TestSuiteDto>) -> Unit,
) {
    showTestSuitesSelectorModal(currentOrganizationName, TestSuiteSelectorPurpose.PUBLIC, initTestSuiteDtos, windowOpenness, testSuiteDtosInSelectorState, setSelectedTestSuiteIds)
}

/**
 * Browse test suites of a given organization.
 *
 * @param currentOrganizationName
 * @param initTestSuiteDtos initial value
 * @param windowOpenness state to control openness of window
 * @param testSuiteDtosInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuiteDtos consumer for result
 */
@Suppress("TYPE_ALIAS")
fun ChildrenBuilder.showPrivateTestSuitesSelectorModal(
    currentOrganizationName: String,
    initTestSuiteDtos: List<TestSuiteDto>,
    windowOpenness: WindowOpenness,
    testSuiteDtosInSelectorState: StateInstance<List<TestSuiteDto>>,
    setSelectedTestSuiteDtos: (List<TestSuiteDto>) -> Unit,
) {
    showTestSuitesSelectorModal(currentOrganizationName, TestSuiteSelectorPurpose.PRIVATE, initTestSuiteDtos, windowOpenness, testSuiteDtosInSelectorState,
        setSelectedTestSuiteDtos)
}

/**
 * Browse test suites for a contest.
 *
 * @param currentOrganizationName
 * @param initTestSuiteDtos initial value
 * @param windowOpenness state to control openness of window
 * @param testSuiteDtosInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuiteDtos consumer for result
 */
@Suppress("TYPE_ALIAS")
fun ChildrenBuilder.showContestTestSuitesSelectorModal(
    currentOrganizationName: String,
    initTestSuiteDtos: List<TestSuiteDto>,
    windowOpenness: WindowOpenness,
    testSuiteDtosInSelectorState: StateInstance<List<TestSuiteDto>>,
    setSelectedTestSuiteDtos: (List<TestSuiteDto>) -> Unit,
) {
    showTestSuitesSelectorModal(currentOrganizationName, TestSuiteSelectorPurpose.CONTEST, initTestSuiteDtos, windowOpenness, testSuiteDtosInSelectorState,
        setSelectedTestSuiteDtos)
}

@Suppress("TOO_MANY_PARAMETERS", "LongParameterList", "TYPE_ALIAS")
private fun ChildrenBuilder.showTestSuitesSelectorModal(
    currentOrganizationName: String,
    selectorPurpose: TestSuiteSelectorPurpose,
    initTestSuiteDtos: List<TestSuiteDto>,
    windowOpenness: WindowOpenness,
    testSuiteDtosInSelectorState: StateInstance<List<TestSuiteDto>>,
    setSelectedTestSuiteDtos: (List<TestSuiteDto>) -> Unit,
) {
    var currentlySelectedTestSuiteDtos by testSuiteDtosInSelectorState
    val onSubmit: () -> Unit = {
        setSelectedTestSuiteDtos(currentlySelectedTestSuiteDtos)
        windowOpenness.closeWindow()
    }
    val onTestSuiteIdUpdate: (List<TestSuiteDto>) -> Unit = {
        currentlySelectedTestSuiteDtos = it
    }
    val onCancel: () -> Unit = {
        currentlySelectedTestSuiteDtos = initTestSuiteDtos
        windowOpenness.closeWindow()
    }
    showTestSuitesSelectorModal(windowOpenness.isOpen(), currentOrganizationName, selectorPurpose, initTestSuiteDtos, onSubmit, onTestSuiteIdUpdate, onCancel)
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TOO_MANY_PARAMETERS",
    "LongParameterList"
)
private fun ChildrenBuilder.showTestSuitesSelectorModal(
    isOpen: Boolean,
    currentOrganizationName: String,
    selectorPurpose: TestSuiteSelectorPurpose,
    preselectedTestSuiteDtos: List<TestSuiteDto>,
    onSubmit: () -> Unit,
    onTestSuiteDtosUpdate: (List<TestSuiteDto>) -> Unit,
    onCancel: () -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = largeTransparentModalStyle
        div {
            className = ClassName("modal-dialog modal-lg modal-dialog-scrollable")
            div {
                className = ClassName("modal-content")
                div {
                    className = ClassName("modal-header")
                    h5 {
                        className = ClassName("modal-title mb-0")
                        +"Test suite selector"
                    }
                    button {
                        type = ButtonType.button
                        className = ClassName("close")
                        asDynamic()["data-dismiss"] = "modal"
                        ariaLabel = "Close"
                        fontAwesomeIcon(icon = faTimesCircle)
                        onClick = {
                            onCancel()
                        }
                    }
                }

                div {
                    className = ClassName("modal-body")
                    testSuiteSelector {
                        this.onTestSuiteUpdate = onTestSuiteDtosUpdate
                        this.preselectedTestSuites = preselectedTestSuiteDtos
                        this.selectorPurpose = selectorPurpose
                        this.currentOrganizationName = currentOrganizationName
                    }
                }

                div {
                    className = ClassName("modal-footer")
                    div {
                        className = ClassName("d-flex justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-primary mt-4")
                            +"Apply"
                            onClick = {
                                onSubmit()
                            }
                        }
                    }
                    div {
                        className = ClassName("d-flex justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-secondary mt-4")
                            +"Cancel"
                            onClick = {
                                onCancel()
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun testSuiteSelector() = FC<TestSuiteSelectorProps> { props ->
    val (currentMode, setCurrentMode) = useState(if (props.preselectedTestSuites.isEmpty()) {
        TestSuiteSelectorMode.BROWSER
    } else {
        TestSuiteSelectorMode.MANAGER
    })
    div {
        className = ClassName("d-flex align-self-center justify-content-around mb-2")
        buttonWithIcon(faAlignJustify, currentMode == TestSuiteSelectorMode.MANAGER, "Manage linked test suites") { setCurrentMode(TestSuiteSelectorMode.MANAGER) }
        buttonWithIcon(faPlus, currentMode == TestSuiteSelectorMode.BROWSER, "Browse public test suites") { setCurrentMode(TestSuiteSelectorMode.BROWSER) }
        buttonWithIcon(faSearch, currentMode == TestSuiteSelectorMode.SEARCH, "Search by name or tag") { setCurrentMode(TestSuiteSelectorMode.SEARCH) }
    }

    useTooltip()

    when (currentMode) {
        TestSuiteSelectorMode.MANAGER -> testSuiteSelectorManagerMode {
            this.onTestSuitesUpdate = props.onTestSuiteUpdate
            this.preselectedTestSuites = props.preselectedTestSuites
            this.selectorPurpose = props.selectorPurpose
            this.currentOrganizationName = props.currentOrganizationName
        }
        TestSuiteSelectorMode.BROWSER -> testSuiteSelectorBrowserMode {
            this.onTestSuitesUpdate = props.onTestSuiteUpdate
            this.preselectedTestSuites = props.preselectedTestSuites
            this.specificOrganizationName = props.specificOrganizationName
            this.selectorPurpose = props.selectorPurpose
            this.currentOrganizationName = props.currentOrganizationName
        }
        TestSuiteSelectorMode.SEARCH -> testSuiteSelectorSearchMode {
            this.onTestSuitesUpdate = props.onTestSuiteUpdate
            this.preselectedTestSuites = props.preselectedTestSuites
            this.selectorPurpose = props.selectorPurpose
            this.currentOrganizationName = props.currentOrganizationName
        }
    }
}
