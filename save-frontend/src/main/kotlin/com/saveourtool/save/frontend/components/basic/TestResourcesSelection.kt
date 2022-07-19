/**
 * Utility methods for creation of the module window for the selection of test resources
 */

@file:Suppress("WildcardImport", "FILE_WILDCARD_IMPORTS")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.frontend.components.views.ProjectView
import com.saveourtool.save.frontend.externals.fontawesome.faQuestionCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.testsuite.TestSuiteDto

import csstype.ClassName
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.events.ChangeEvent
import react.dom.html.InputType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h6
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.label
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

    // properties for CUSTOM_TESTS mode
    var gitUrlFromInputField: String
    var gitBranchOrCommitFromInputField: String
    var execCmd: String
    var batchSizeForAnalyzer: String
    var testRootPath: String

    // properties for STANDARD_BENCHMARKS mode
    var standardTestSuites: List<TestSuiteDto>
    var selectedStandardSuites: MutableList<String>
    var selectedLanguageForStandardTests: String?
}

@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
private fun ChildrenBuilder.setAdditionalPropertiesForStandardMode(
    value: String,
    placeholder: String,
    tooltipText: String,
    labelText: String,
    inputType: InputType,
    onChangeFunc: (ChangeEvent<HTMLInputElement>) -> Unit
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
            onChangeFunc(it)
        }
    }
}

/**
 * @param updateGitUrlFromInputField
 * @param updateGitBranchOrCommitInputField
 * @param updateTestRootPath
 * @param setSelectedLanguageForStandardTests
 * @param setExecCmd
 * @param setBatchSize
 * @return an Component
 */
@Suppress(
    "LongMethod",
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
    "LongParameterList",
)
fun testResourcesSelection(
    updateGitUrlFromInputField: (ChangeEvent<HTMLInputElement>) -> Unit,
    updateGitBranchOrCommitInputField: (ChangeEvent<HTMLInputElement>) -> Unit,
    updateTestRootPath: (ChangeEvent<HTMLInputElement>) -> Unit,
    setExecCmd: (ChangeEvent<HTMLInputElement>) -> Unit,
    setBatchSize: (ChangeEvent<HTMLInputElement>) -> Unit,
    setSelectedLanguageForStandardTests: (String) -> Unit,
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
            div {
                className = ClassName("input-group-sm mb-3")
                div {
                    className = ClassName("row")
                    sup {
                        className = ClassName("tooltip-and-popover")
                        tabIndex = 0
                        fontAwesomeIcon(icon = faQuestionCircle)
                        asDynamic()["tooltip-placement"] = "top"
                        asDynamic()["tooltip-title"] = ""
                        asDynamic()["popover-placement"] = "left"
                        asDynamic()["popover-title"] =
                                "Use the following link to read more about save format:"
                        asDynamic()["popover-content"] =
                                "<a href =\"https://github.com/saveourtool/save-cli/blob/main/README.md\" > SAVE core README </a>"
                        asDynamic()["data-trigger"] = "focus"
                    }
                    h6 {
                        className = ClassName("d-inline ml-2")
                        +"Git Url of your test suites (in save format):"
                    }
                }
                div {
                    className = ClassName("input-group-prepend")
                    input {
                        type = InputType.text
                        className =
                                if (props.gitUrlFromInputField.isBlank() && props.isSubmitButtonPressed!!) {
                                    ClassName("form-control is-invalid")
                                } else {
                                    ClassName("form-control")
                                }
                        if (props.gitUrlFromInputField.isNotBlank()) {
                            defaultValue = props.gitUrlFromInputField
                        }

                        placeholder = "https://github.com/my-project"
                        onChange = {
                            updateGitUrlFromInputField(it)
                        }
                    }
                }
            }

            div {
                className = ClassName("input-group-sm")
                div {
                    className = ClassName("row")
                    sup {
                        className = ClassName("tooltip-and-popover")
                        fontAwesomeIcon(icon = faQuestionCircle)
                        tabIndex = 0
                        asDynamic()["tooltip-placement"] = "top"
                        asDynamic()["tooltip-title"] = ""
                        asDynamic()["popover-placement"] = "left"
                        asDynamic()["popover-title"] = "Keep in mind the following rules:"
                        asDynamic()["popover-content"] = "Provide full name of your brach with `origin` prefix: origin/your_branch." +
                                " Or in aim to use the concrete commit just provide hash of it."
                        asDynamic()["data-trigger"] = "focus"
                    }
                    h6 {
                        className = ClassName("d-inline ml-2")
                        +"Git branch or specific commit in your repository:"
                    }
                }
                div {
                    className = ClassName("input-group-prepend")
                    input {
                        type = InputType.text
                        name = "itemText"
                        key = "itemText"
                        className = ClassName("form-control")
                        if (props.gitBranchOrCommitFromInputField.isNotBlank()) {
                            defaultValue = props.gitBranchOrCommitFromInputField
                        }
                        placeholder = "leave empty if you would like to use default branch with latest commit"
                        onChange = {
                            updateGitBranchOrCommitInputField(it)
                        }
                    }
                }
            }

            div {
                className = ClassName("input-group-sm mt-3")
                div {
                    className = ClassName("row")
                    sup {
                        className = ClassName("tooltip-and-popover")
                        tabIndex = 0
                        fontAwesomeIcon(icon = faQuestionCircle)
                        asDynamic()["tooltip-placement"] = "top"
                        asDynamic()["tooltip-title"] = ""
                        asDynamic()["popover-placement"] = "left"
                        asDynamic()["popover-title"] = "Relative path to the root directory with tests"
                        asDynamic()["popover-content"] = ProjectView.TEST_ROOT_DIR_HINT
                        asDynamic()["data-trigger"] = "focus"
                    }
                    h6 {
                        className = ClassName("d-inline ml-2")
                        +"Relative path (to the root directory) of the test suites in the repo:"
                    }
                }
                div {
                    className = ClassName("input-group-prepend")
                    input {
                        type = InputType.text
                        name = "itemText"
                        key = "itemText"
                        className = ClassName("form-control")
                        value = props.testRootPath
                        placeholder = "leave empty if tests are in the repository root"
                        onChange = {
                            updateTestRootPath(it)
                        }
                    }
                }
            }
        }
    }

    div {
        className = ClassName(cardStyleByTestingType(props, TestingType.STANDARD_BENCHMARKS))
        div {
            className = ClassName("card-body")
            suitesTable(
                props.standardTestSuites,
                props.selectedLanguageForStandardTests,
                setSelectedLanguageForStandardTests
            )

            setAdditionalPropertiesForStandardMode(
                props.execCmd,
                "Execution command",
                "Execution command that will be used to run the tool and tests",
                "",
                InputType.text,
                setExecCmd
            )
            val toolTipTextForBatchSize = "Batch size controls how many files will be processed at the same time." +
                    " To know more about batch size, please visit: https://github.com/saveourtool/save."
            setAdditionalPropertiesForStandardMode(
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
            className = ClassName("card-body control-label col-auto justify-content-between justify-content-center font-weight-bold text-danger mb-4 pl-0")
            +"Stay turned! Soon you will be able to run your tool in contest mode!"
        }
    }
}

private fun cardStyleByTestingType(props: TestResourcesProps, testingType: TestingType) =
        if (props.testingType == testingType) "card shadow mb-4 w-100" else "d-none"
