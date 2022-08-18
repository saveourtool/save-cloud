/**
 * Component for selecting test suites
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.testsuiteselector

import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.externals.modal.*
import com.saveourtool.save.frontend.utils.WindowOpenness

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
     * If it's null we show public tests
     */
    var specificOrganizationName: String?
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
 * @param initTestSuiteIds initial value
 * @param setSelectedTestSuiteIds consumer for result
 * @param windowOpenness state to control openness of window
 * @param testSuiteIdsInSelectorState state for intermediate result in selector
 */
fun ChildrenBuilder.showPublicTestSuitesSelectorModal(
    initTestSuiteIds: List<Long>,
    setSelectedTestSuiteIds: (List<Long>) -> Unit,
    windowOpenness: WindowOpenness,
    testSuiteIdsInSelectorState: StateInstance<List<Long>>,
) {
    showTestSuitesSelectorModal(null, initTestSuiteIds, setSelectedTestSuiteIds, windowOpenness, testSuiteIdsInSelectorState)
}

/**
 * @param organizationName
 * @param initTestSuiteIds initial value
 * @param setSelectedTestSuiteIds consumer for result
 * @param windowOpenness state to control openness of window
 * @param testSuiteIdsInSelectorState state for intermediate result in selector
 */
fun ChildrenBuilder.showPrivateTestSuitesSelectorModal(
    organizationName: String,
    initTestSuiteIds: List<Long>,
    setSelectedTestSuiteIds: (List<Long>) -> Unit,
    windowOpenness: WindowOpenness,
    testSuiteIdsInSelectorState: StateInstance<List<Long>>,
) {
    showTestSuitesSelectorModal(organizationName, initTestSuiteIds, setSelectedTestSuiteIds, windowOpenness, testSuiteIdsInSelectorState)
}

private fun ChildrenBuilder.showTestSuitesSelectorModal(
    specificOrganizationName: String?,
    initTestSuiteIds: List<Long>,
    setSelectedTestSuiteIds: (List<Long>) -> Unit,
    windowOpenness: WindowOpenness,
    testSuiteIdsInSelectorState: StateInstance<List<Long>>,
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
    showTestSuitesSelectorModal(windowOpenness.isOpen(), specificOrganizationName, initTestSuiteIds, onSubmit, onTestSuiteIdUpdate, onCancel)
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun ChildrenBuilder.showTestSuitesSelectorModal(
    isOpen: Boolean,
    specificOrganizationName: String?,
    preselectedTestSuiteIds: List<Long>,
    onSubmit: () -> Unit,
    onTestSuiteIdUpdate: (List<Long>) -> Unit,
    onCancel: () -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = transparentModalStyle
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

private fun ChildrenBuilder.buildButton(
    icon: FontAwesomeIconModule,
    isActive: Boolean,
    tooltipText: String,
    onClickFun: () -> Unit
) {
    button {
        type = ButtonType.button
        title = tooltipText
        val active = if (isActive) {
            "active"
        } else {
            ""
        }
        className = ClassName("btn btn-outline-secondary $active")
        fontAwesomeIcon(icon = icon)
        onClick = {
            onClickFun()
        }

        val jquery = kotlinext.js.require("jquery")
        kotlinext.js.require("popper.js")
        kotlinext.js.require("bootstrap")
        asDynamic()["data-toggle"] = "tooltip"
        asDynamic()["data-placement"] = "bottom"
        jquery("[data-toggle=\"tooltip\"]").tooltip()
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
        buildButton(faAlignJustify, currentMode == TestSuiteSelectorMode.MANAGER, "Manage linked test suites") { setCurrentMode(TestSuiteSelectorMode.MANAGER) }
        buildButton(faPlus, currentMode == TestSuiteSelectorMode.BROWSER, "Browse public test suites") { setCurrentMode(TestSuiteSelectorMode.BROWSER) }
        buildButton(faSearch, currentMode == TestSuiteSelectorMode.SEARCH, "Search by name or tag") { setCurrentMode(TestSuiteSelectorMode.SEARCH) }
    }
    when (currentMode) {
        TestSuiteSelectorMode.MANAGER -> testSuiteSelectorManagerMode {
            this.onTestSuiteIdsUpdate = props.onTestSuiteIdUpdate
            this.preselectedTestSuiteIds = props.preselectedTestSuiteIds
        }
        TestSuiteSelectorMode.BROWSER -> testSuiteSelectorBrowserMode {
            this.onTestSuiteIdsUpdate = props.onTestSuiteIdUpdate
            this.preselectedTestSuiteIds = props.preselectedTestSuiteIds
            this.specificOrganizationName = props.specificOrganizationName
        }
        TestSuiteSelectorMode.SEARCH -> testSuiteSelectorSearchMode {
            this.onTestSuiteIdsUpdate = props.onTestSuiteIdUpdate
            this.preselectedTestSuiteIds = props.preselectedTestSuiteIds
        }
    }
}
