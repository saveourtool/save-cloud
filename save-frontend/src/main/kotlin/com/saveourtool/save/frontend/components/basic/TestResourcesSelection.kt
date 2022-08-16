/**
 * Utility methods for creation of the module window for the selection of test resources
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.components.basic.testsuiteselector.showTestSuiteSelectorModal
import com.saveourtool.save.frontend.components.views.ProjectView
import com.saveourtool.save.frontend.externals.fontawesome.faQuestionCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.testsuite.TestSuiteDto

import csstype.ClassName
import react.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.option
import react.dom.html.ReactHTML.select
import react.dom.html.ReactHTML.sup

private val checkBox = checkBoxGrid()

/**
 * Types of testing (that can be selected by user)
 */
enum class TestingType {
    CONTEST_MODE,
    CUSTOM_TESTS,
    STANDARD_BENCHMARKS,
    ;
}

/**
 * Properties for test resources
 */
@Suppress("MISSING_KDOC_CLASS_ELEMENTS")
external interface TestResourcesProps : PropsWithChildren {
    var testingType: TestingType
    var isSubmitButtonPressed: Boolean?
    var gitDto: GitDto?

    // properties for CONTEST_MODE
    var projectName: String
    var organizationName: String
    var onContestEnrollerResponse: (String) -> Unit
    var availableContests: List<ContestDto>
    var selectedContest: ContestDto

    // properties for CUSTOM_TESTS mode
    var selectedTestSuiteIds: List<Long>
    var execCmd: String
    var batchSizeForAnalyzer: String

    // properties for STANDARD_BENCHMARKS mode
    var standardTestSuites: List<TestSuiteDto>
    var selectedStandardSuites: MutableList<String>
    var selectedLanguageForStandardTests: String?
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

/**
 * @param setTestSuiteIds
 * @param setSelectedLanguageForStandardTests
 * @param setExecCmd
 * @param setBatchSize
 * @param updateContestFromInputField
 * @return an Component
 */
@Suppress(
    "LongMethod",
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
)
fun testResourcesSelection(
    setTestSuiteIds: (List<Long>) -> Unit,
    setExecCmd: (String) -> Unit,
    setBatchSize: (String) -> Unit,
    setSelectedLanguageForStandardTests: (String) -> Unit,
    updateContestFromInputField: (ContestDto) -> Unit,
) = FC<TestResourcesProps> { props ->
    val (isContestEnrollerOpen, setIsContestEnrollerOpen) = useState(false)
    showContestEnrollerModal(
        isContestEnrollerOpen,
        ProjectNameProps(props.organizationName, props.projectName),
        { setIsContestEnrollerOpen(false) },
    ) {
        setIsContestEnrollerOpen(false)
        props.onContestEnrollerResponse(it)
    }
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

    div {
        className = ClassName(cardStyleByTestingType(props, TestingType.CUSTOM_TESTS))

        div {
            className = ClassName("card-body ")

            addAdditionalProperty(
                props.execCmd,
                "Execution command",
                "Execution command that will be used to run the tool and tests",
                "",
                InputType.text,
                setExecCmd
            )
            val toolTipTextForBatchSize = "Batch size controls how many files will be processed at the same time." +
                    " To know more about batch size, please visit: https://github.com/saveourtool/save."
            addAdditionalProperty(
                props.batchSizeForAnalyzer,
                "",
                toolTipTextForBatchSize,
                "Batch size (default: 1):",
                InputType.number,
                setBatchSize
            )

            val (isTestSuiteSelectorOpen, setIsTestSuiteSelectorOpen) = useState(false)
            val (selectedTestSuiteIds, setSelectedTestSuiteIds) = useState(emptyList<Long>())
            showTestSuiteSelectorModal(
                isTestSuiteSelectorOpen,
                props.selectedTestSuiteIds,
                onSubmit = {
                    setTestSuiteIds(selectedTestSuiteIds)
                    setIsTestSuiteSelectorOpen(false)
                },
                onTestSuiteIdUpdate = {
                    setSelectedTestSuiteIds(it)
                },
                onCancel = {
                    setSelectedTestSuiteIds(emptyList())
                    setIsTestSuiteSelectorOpen(false)
                }
            )

            // ==== Contest test suites
            div {
                className = ClassName("mt-2")
                inputTextFormRequired(
                    InputTypes.TEST_SUITE_IDS,
                    selectedTestSuiteIds.joinToString(", "),
                    true,
                    "col-12 pl-2 pr-2",
                    "Test Suite Ids",
                    onClickFun = { setIsTestSuiteSelectorOpen(true) }
                )
            }
        }
    }

    div {
        className = ClassName(cardStyleByTestingType(props, TestingType.STANDARD_BENCHMARKS))
        div {
            className = ClassName("card-body")
            suitesTable {
                selectedStandardSuites = props.selectedStandardSuites
                suites = props.standardTestSuites
                selectedLanguageForStandardTests = props.selectedLanguageForStandardTests
                this.setSelectedLanguageForStandardTests = setSelectedLanguageForStandardTests
            }

            addAdditionalProperty(
                props.execCmd,
                "Execution command",
                "Execution command that will be used to run the tool and tests",
                "",
                InputType.text,
                setExecCmd
            )
            val toolTipTextForBatchSize = "Batch size controls how many files will be processed at the same time." +
                    " To know more about batch size, please visit: https://github.com/saveourtool/save."
            addAdditionalProperty(
                props.batchSizeForAnalyzer,
                "",
                toolTipTextForBatchSize,
                "Batch size (default: 1):",
                InputType.number,
                setBatchSize
            )

            checkBox {
                selectedStandardSuites = props.selectedStandardSuites
                rowSize = ProjectView.TEST_SUITE_ROW
                suites = props.standardTestSuites
                selectedLanguageForStandardTests = props.selectedLanguageForStandardTests
            }
        }
    }

    div {
        className = ClassName(cardStyleByTestingType(props, TestingType.CONTEST_MODE))
        div {
            className = ClassName("card-body d-flex justify-content-center")
            button {
                className = ClassName("d-flex justify-content-center btn btn-primary")
                +"Enroll for a contest"
                onClick = {
                    setIsContestEnrollerOpen(true)
                }
            }
        }
    }
    if (props.testingType == TestingType.CONTEST_MODE) {
        label {
            className = ClassName("control-label col-auto justify-content-between justify-content-center font-weight-bold text-gray-800 mb-4 pl-0")
            +"4. Run your tool on private tests and see your score"
        }
    }
    div {
        className = ClassName(cardStyleByTestingType(props, TestingType.CONTEST_MODE))
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
                    updateContestFromInputField(selectedContest)
                }
            }
        }
    }
}

private fun cardStyleByTestingType(props: TestResourcesProps, testingType: TestingType) =
        if (props.testingType == testingType) "card shadow mb-4 w-100" else "d-none"
