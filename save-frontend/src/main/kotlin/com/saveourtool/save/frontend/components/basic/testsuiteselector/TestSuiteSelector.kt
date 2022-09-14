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
    var onTestSuiteIdUpdate: (List<Long>) -> Unit

    /**
     * List of test suite ids that should be preselected
     */
    var preselectedTestSuiteIds: List<Long>

    /**
     * Specific organization name which reduces list of test suites source.
     * If null, all the test suites are shown
     */
    var specificOrganizationName: String?

    /**
     * Mode that defines what kind of test suites will be shown
     */
    var selectorPurpose: TestSuiteSelectorPurpose
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

    /**
     * Will later be merged with PUBLIC
     */
    STANDARD,
    ;
}

/**
 * Browse standard test suites.
 *
 * @param initTestSuiteIds initial value
 * @param windowOpenness state to control openness of window
 * @param testSuiteIdsInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuiteIds consumer for result
 */
fun ChildrenBuilder.showPublicTestSuitesSelectorModal(
    initTestSuiteIds: List<Long>,
    windowOpenness: WindowOpenness,
    testSuiteIdsInSelectorState: StateInstance<List<Long>>,
    setSelectedTestSuiteIds: (List<Long>) -> Unit,
) {
    showTestSuitesSelectorModal(null, TestSuiteSelectorPurpose.STANDARD, initTestSuiteIds, windowOpenness, testSuiteIdsInSelectorState, setSelectedTestSuiteIds)
}

/**
 * Browse all the available test suites.
 *
 * @param initTestSuiteIds initial value
 * @param windowOpenness state to control openness of window
 * @param testSuiteIdsInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuiteIds consumer for result
 */
fun ChildrenBuilder.showGeneralTestSuitesSelectorModal(
    initTestSuiteIds: List<Long>,
    windowOpenness: WindowOpenness,
    testSuiteIdsInSelectorState: StateInstance<List<Long>>,
    setSelectedTestSuiteIds: (List<Long>) -> Unit,
) {
    showTestSuitesSelectorModal(null, TestSuiteSelectorPurpose.PUBLIC, initTestSuiteIds, windowOpenness, testSuiteIdsInSelectorState, setSelectedTestSuiteIds)
}

/**
 * Browse test suites of a given organization.
 *
 * @param organizationName
 * @param initTestSuiteIds initial value
 * @param windowOpenness state to control openness of window
 * @param testSuiteIdsInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuiteIds consumer for result
 */
fun ChildrenBuilder.showPrivateTestSuitesSelectorModal(
    organizationName: String,
    initTestSuiteIds: List<Long>,
    windowOpenness: WindowOpenness,
    testSuiteIdsInSelectorState: StateInstance<List<Long>>,
    setSelectedTestSuiteIds: (List<Long>) -> Unit,
) {
    showTestSuitesSelectorModal(organizationName, TestSuiteSelectorPurpose.PRIVATE, initTestSuiteIds, windowOpenness, testSuiteIdsInSelectorState, setSelectedTestSuiteIds)
}

/**
 * Browse test suites for a contest.
 *
 * @param initTestSuiteIds initial value
 * @param windowOpenness state to control openness of window
 * @param testSuiteIdsInSelectorState state for intermediate result in selector
 * @param setSelectedTestSuiteIds consumer for result
 */
fun ChildrenBuilder.showContestTestSuitesSelectorModal(
    initTestSuiteIds: List<Long>,
    windowOpenness: WindowOpenness,
    testSuiteIdsInSelectorState: StateInstance<List<Long>>,
    setSelectedTestSuiteIds: (List<Long>) -> Unit,
) {
    showTestSuitesSelectorModal(null, TestSuiteSelectorPurpose.CONTEST, initTestSuiteIds, windowOpenness, testSuiteIdsInSelectorState, setSelectedTestSuiteIds)
}

@Suppress("TOO_MANY_PARAMETERS", "LongParameterList")
private fun ChildrenBuilder.showTestSuitesSelectorModal(
    specificOrganizationName: String?,
    selectorPurpose: TestSuiteSelectorPurpose,
    initTestSuiteIds: List<Long>,
    windowOpenness: WindowOpenness,
    testSuiteIdsInSelectorState: StateInstance<List<Long>>,
    setSelectedTestSuiteIds: (List<Long>) -> Unit,
) {
    var currentlySelectedTestSuiteIds by testSuiteIdsInSelectorState
    val onSubmit: () -> Unit = {
        setSelectedTestSuiteIds(currentlySelectedTestSuiteIds)
        windowOpenness.closeWindow()
    }
    val onTestSuiteIdUpdate: (List<Long>) -> Unit = {
        currentlySelectedTestSuiteIds = it
    }
    val onCancel: () -> Unit = {
        currentlySelectedTestSuiteIds = initTestSuiteIds
        windowOpenness.closeWindow()
    }
    showTestSuitesSelectorModal(windowOpenness.isOpen(), specificOrganizationName, selectorPurpose, initTestSuiteIds, onSubmit, onTestSuiteIdUpdate, onCancel)
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "TOO_MANY_PARAMETERS",
    "LongParameterList"
)
private fun ChildrenBuilder.showTestSuitesSelectorModal(
    isOpen: Boolean,
    specificOrganizationName: String?,
    selectorPurpose: TestSuiteSelectorPurpose,
    preselectedTestSuiteIds: List<Long>,
    onSubmit: () -> Unit,
    onTestSuiteIdUpdate: (List<Long>) -> Unit,
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
                        this.onTestSuiteIdUpdate = onTestSuiteIdUpdate
                        this.preselectedTestSuiteIds = preselectedTestSuiteIds
                        this.specificOrganizationName = specificOrganizationName
                        this.selectorPurpose = selectorPurpose
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
    val (currentMode, setCurrentMode) = useState(if (props.preselectedTestSuiteIds.isEmpty()) {
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
            this.onTestSuiteIdsUpdate = props.onTestSuiteIdUpdate
            this.preselectedTestSuiteIds = props.preselectedTestSuiteIds
            this.selectorPurpose = props.selectorPurpose
        }
        TestSuiteSelectorMode.BROWSER -> testSuiteSelectorBrowserMode {
            this.onTestSuiteIdsUpdate = props.onTestSuiteIdUpdate
            this.preselectedTestSuiteIds = props.preselectedTestSuiteIds
            this.specificOrganizationName = props.specificOrganizationName
            this.selectorPurpose = props.selectorPurpose
        }
        TestSuiteSelectorMode.SEARCH -> testSuiteSelectorSearchMode {
            this.onTestSuiteIdsUpdate = props.onTestSuiteIdUpdate
            this.preselectedTestSuiteIds = props.preselectedTestSuiteIds
            this.selectorPurpose = props.selectorPurpose
        }
    }
}
