/**
 * Utility methods for creation of the module window for the selection of test resources
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.basic.testsuiteselector.showPrivateTestSuitesSelectorModal
import com.saveourtool.save.frontend.components.basic.testsuiteselector.showPublicTestSuitesSelectorModal
import com.saveourtool.save.frontend.utils.WindowOpenness

import csstype.ClassName
import react.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select

val testResourcesSelection = prepareTestResourcesSelection()

/**
 * Types of testing (that can be selected by user)
 */
enum class TestingType {
    CONTEST_MODE,
    PRIVATE_TESTS,
    PUBLIC_TESTS,
    ;
}

/**
 * Properties for test resources
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface TestResourcesProps : PropsWithChildren {
    var testingType: TestingType
    var isSubmitButtonPressed: Boolean?

    // properties for CONTEST_MODE
    var projectName: String
    var organizationName: String
    var onContestEnrollerResponse: (String) -> Unit
    var availableContests: List<ContestDto>
    var selectedContest: ContestDto
    var setSelectedContest: (ContestDto) -> Unit

    // properties for PRIVATE_TESTS mode
    var selectedPrivateTestSuiteIds: List<Long>
    var setSelectedPrivateTestSuiteIds: (List<Long>) -> Unit

    // properties for PUBLIC_TESTS mode
    var selectedPublicTestSuiteIds: List<Long>
    var setSelectedPublicTestSuiteIds: (List<Long>) -> Unit
    var execCmd: String
    var setExecCmd: (String) -> Unit
    var batchSizeForAnalyzer: String
    var setBatchSizeForAnalyzer: (String) -> Unit
}

@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
private fun ChildrenBuilder.addAdditionalProperty(
    value: String,
    placeholder: String,
    tooltipText: String,
    labelText: String,
    inputType: InputType,
    onChangeFunc: (String) -> Unit
) = div {
    className = ClassName("input-group mb-3")
    if (labelText.isNotEmpty()) {
        div {
            className = ClassName("input-group-prepend")
            label {
                className = ClassName("input-group-text")
                +labelText
            }
        }
    }

    input {
        type = inputType
        name = "itemText"
        // workaround to have a default value for Batch field
        if (labelText.isNotEmpty()) {
            defaultValue = "1"
        }
        key = "itemText"
        @Suppress("MAGIC_NUMBER")
        min = 1.0
        className = ClassName("form-control")
        if (tooltipText.isNotBlank()) {
            asDynamic()["data-toggle"] = "tooltip"
            asDynamic()["data-placement"] = "right"
            title = tooltipText
        }
        this.value = value
        this.placeholder = placeholder
        onChange = {
            onChangeFunc(it.target.value)
        }
    }
}

private fun ContestDto.label(): String = "$organizationName/$name"

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun ChildrenBuilder.renderForPublicAndPrivateTests(
    props: TestResourcesProps,
    testSuiteSelectorWindowOpenness: WindowOpenness,
    testSuiteIdsInSelectorState: StateInstance<List<Long>>,
    selectedTestSuiteIds: List<Long>,
    setSelectedTestSuiteIds: (List<Long>) -> Unit,
) {
    div {
        className = ClassName("card shadow mb-4 w-100")

        div {
            className = ClassName("card-body ")

            addAdditionalProperty(
                props.execCmd,
                "Execution command",
                "Execution command that will be used to run the tool and tests",
                "",
                InputType.text,
                props.setExecCmd
            )
            val toolTipTextForBatchSize = "Batch size controls how many files will be processed at the same time." +
                    " To know more about batch size, please visit: https://github.com/saveourtool/save."
            addAdditionalProperty(
                props.batchSizeForAnalyzer,
                "",
                toolTipTextForBatchSize,
                "Batch size (default: 1):",
                InputType.number,
                props.setBatchSizeForAnalyzer
            )

            when (props.testingType) {
                TestingType.PRIVATE_TESTS -> showPrivateTestSuitesSelectorModal(
                    props.organizationName,
                    selectedTestSuiteIds,
                    testSuiteSelectorWindowOpenness,
                    testSuiteIdsInSelectorState,
                    setSelectedTestSuiteIds
                )
                TestingType.PUBLIC_TESTS -> showPublicTestSuitesSelectorModal(
                    selectedTestSuiteIds,
                    testSuiteSelectorWindowOpenness,
                    testSuiteIdsInSelectorState,
                    setSelectedTestSuiteIds
                )
                else -> throw IllegalStateException("Not supported testingType ${props.testingType}")
            }

            // ==== test suite ids selector
            div {
                className = ClassName("mt-2")
                inputTextFormRequired(
                    InputTypes.TEST_SUITE_IDS,
                    selectedTestSuiteIds.joinToString(", "),
                    true,
                    "col-12 pl-2 pr-2",
                    "Test Suite Ids",
                    onClickFun = testSuiteSelectorWindowOpenness.openWindowAction()
                )
            }
        }
    }
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod")
private fun ChildrenBuilder.renderForContestMode(
    props: TestResourcesProps,
    contestEnrollerWindowOpenness: WindowOpenness,
) {
    showContestEnrollerModal(
        contestEnrollerWindowOpenness.isOpen(),
        ProjectNameProps(props.organizationName, props.projectName),
        contestEnrollerWindowOpenness.closeWindowAction(),
    ) {
        contestEnrollerWindowOpenness.closeWindow()
        props.onContestEnrollerResponse(it)
    }
    div {
        className = ClassName("card shadow mb-4 w-100")
        div {
            className = ClassName("card-body d-flex justify-content-center")
            button {
                className = ClassName("d-flex justify-content-center btn btn-primary")
                +"Enroll for a contest"
                onClick = {
                    contestEnrollerWindowOpenness.openWindow()
                }
            }
        }
    }
    label {
        className = ClassName("control-label col-auto justify-content-between justify-content-center font-weight-bold text-gray-800 mb-4 pl-0")
        +"4. Run your tool on private tests and see your score"
    }
    div {
        className = ClassName("card shadow mb-4 w-100")
        div {
            className = ClassName("input-group-prepend")

            select {
                className = ClassName("form-control")
                props.availableContests.forEach {
                    option {
                        +it.label()
                    }
                }
                required = true
                value = props.selectedContest.label()
                onChange = { event ->
                    val selectedContestLabel = event.target.value
                    val selectedContest = requireNotNull(props.availableContests.find { it.label() == selectedContestLabel }) {
                        "Invalid contest is selected $selectedContestLabel"
                    }
                    props.setSelectedContest(selectedContest)
                }
            }
        }
    }
}

/**
 * @return a Component
 */
@Suppress(
    "LongMethod",
    "TOO_LONG_FUNCTION",
)
fun prepareTestResourcesSelection() = FC<TestResourcesProps> { props ->
    // states for private mode
    val testSuiteSelectorWindowOpennessPrivateMode = WindowOpenness.create()
    val testSuiteIdsInSelectorStatePrivateMode = useState(emptyList<Long>())
    // states for public mode
    val testSuiteSelectorWindowOpennessPublicMode = WindowOpenness.create()
    val testSuiteIdsInSelectorStatePublicMode = useState(emptyList<Long>())
    // states for contest mode
    val contestEnrollerWindowOpenness = WindowOpenness.create()

    if (props.testingType == TestingType.CONTEST_MODE) {
        label {
            className = ClassName("control-label col-auto justify-content-between justify-content-center font-weight-bold text-gray-800 mb-4 pl-0")
            +"3. Enroll for a contest"
        }
    } else {
        label {
            className = ClassName("control-label col-auto justify-content-between font-weight-bold text-gray-800 mb-4 pl-0")
            +"3. Specify test-resources that will be used for testing:"
        }
    }

    when (props.testingType) {
        TestingType.PRIVATE_TESTS -> renderForPublicAndPrivateTests(
            props,
            testSuiteSelectorWindowOpennessPrivateMode,
            testSuiteIdsInSelectorStatePrivateMode,
            props.selectedPrivateTestSuiteIds,
            props.setSelectedPrivateTestSuiteIds
        )
        TestingType.PUBLIC_TESTS -> renderForPublicAndPrivateTests(
            props,
            testSuiteSelectorWindowOpennessPublicMode,
            testSuiteIdsInSelectorStatePublicMode,
            props.selectedPublicTestSuiteIds,
            props.setSelectedPublicTestSuiteIds
        )
        TestingType.CONTEST_MODE -> renderForContestMode(
            props,
            contestEnrollerWindowOpenness
        )
    }
}
