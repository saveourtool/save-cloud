/**
 * This file contains a modal window to fetch TestSuitesSource and related classes
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.frontend.components.basic.testsuitessources.fetch

import com.saveourtool.save.frontend.components.basic.selectFormRequired
import com.saveourtool.save.frontend.components.inputform.InputTypes
import com.saveourtool.save.frontend.components.inputform.inputTextFormRequired
import com.saveourtool.save.frontend.components.modal.largeTransparentModalStyle
import com.saveourtool.save.frontend.components.modal.modal
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import csstype.ClassName
import react.*
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val innerTestSuitesSourceFetcher = innerTestSuitesSourceFetcher()
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val tagSelector = selectFormRequired<String>()
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
private val branchSelector = selectFormRequired<String>()

/**
 * Properties for testSuitesSourceFetcher
 */
external interface TestSuitesSourceFetcherProps : Props {
    /**
     * Control openness of modal window
     */
    var windowOpenness: WindowOpenness

    /**
     * [TestSuitesSourceDto] to be fetched
     */
    var testSuitesSource: TestSuitesSourceDto

    /**
     * Selected fetch mode
     */
    var selectedFetchModeState: StateInstance<TestSuitesSourceFetchMode>

    /**
     * Selected value
     */
    var selectedValueState: StateInstance<String?>
}

/**
 * @param windowOpenness
 * @param testSuitesSource
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
)
fun ChildrenBuilder.testSuitesSourceFetcher(
    windowOpenness: WindowOpenness,
    testSuitesSource: TestSuitesSourceDto,
) {
    val selectedFetchModeState = useState(TestSuitesSourceFetchMode.BY_TAG)
    val (selectedFetchMode, _) = selectedFetchModeState
    val selectedValueState: StateInstance<String?> = useState<String>()
    val (selectedValue, _) = selectedValueState
    val triggerFetchTestSuiteSource = useDeferredRequest {
        post(
            url = with(testSuitesSource) {
                "$apiUrl/test-suites-sources/$organizationName/${encodeURIComponent(name)}/fetch?mode=$selectedFetchMode&version=$selectedValue"
            },
            headers = jsonHeaders,
            loadingHandler = ::loadingHandler,
            body = undefined
        )
    }

    modal { modalProps ->
        modalProps.isOpen = windowOpenness.isOpen()
        modalProps.style = largeTransparentModalStyle
        div {
            className = ClassName("modal-dialog modal-lg modal-dialog-scrollable")
            div {
                className = ClassName("modal-content")
                div {
                    className = ClassName("modal-header")
                    h5 {
                        className = ClassName("modal-title mb-0")
                        +"Test suites source fetcher"
                    }
                    button {
                        type = ButtonType.button
                        className = ClassName("close")
                        asDynamic()["data-dismiss"] = "modal"
                        ariaLabel = "Close"
                        fontAwesomeIcon(icon = faTimesCircle)
                        onClick = windowOpenness.closeWindowAction().withUnusedArg()
                    }
                }

                div {
                    className = ClassName("modal-body")
                    innerTestSuitesSourceFetcher {
                        this.windowOpenness = windowOpenness
                        this.testSuitesSource = testSuitesSource
                        this.selectedFetchModeState = selectedFetchModeState
                        this.selectedValueState = selectedValueState
                    }
                }

                div {
                    className = ClassName("modal-footer")
                    div {
                        className = ClassName("d-flex justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-primary mt-4")
                            +"Fetch"
                            onClick = {
                                triggerFetchTestSuiteSource()
                                windowOpenness.closeWindow()
                            }
                        }
                    }
                    div {
                        className = ClassName("d-flex justify-content-center")
                        button {
                            type = ButtonType.button
                            className = ClassName("btn btn-secondary mt-4")
                            +"Cancel"
                            onClick = windowOpenness.closeWindowAction().withUnusedArg()
                        }
                    }
                }
            }
        }
    }
}

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
)
private fun innerTestSuitesSourceFetcher() = FC<TestSuitesSourceFetcherProps> { props ->
    val (selectedFetchMode, _) = props.selectedFetchModeState
    val (selectedValue, setSelectedValue) = props.selectedValueState

    div {
        className = ClassName("d-flex align-self-center justify-content-around mb-2")
        buttonWithIcon(
            icon = faTag,
            tooltipText = "Fetch test suites source by tag",
            buttonMode = TestSuitesSourceFetchMode.BY_TAG,
            currentModeState = props.selectedFetchModeState
        ) {
            setSelectedValue(null)
        }
        buttonWithIcon(
            icon = faCodeBranch,
            tooltipText = "Fetch test suites source by branch",
            buttonMode = TestSuitesSourceFetchMode.BY_BRANCH,
            currentModeState = props.selectedFetchModeState
        ) {
            setSelectedValue(null)
        }
        buttonWithIcon(
            icon = faCheckCircle,
            tooltipText = "Fetch test suites source by commit",
            buttonMode = TestSuitesSourceFetchMode.BY_COMMIT,
            currentModeState = props.selectedFetchModeState
        ) {
            setSelectedValue(null)
        }
    }
    useTooltip()

    val urlPrefix = with(props.testSuitesSource) {
        "$apiUrl/test-suites-sources/$organizationName/${encodeURIComponent(name)}"
    }
    when (selectedFetchMode) {
        TestSuitesSourceFetchMode.BY_TAG -> div {
            tagSelector {
                formType = InputTypes.SOURCE_TAG
                validInput = true
                classes = "mb-2"
                formName = "Source tag:"
                getData = {
                    get(
                        url = "$urlPrefix/tag-list-to-fetch",
                        headers = jsonHeaders,
                        loadingHandler = ::loadingHandler,
                    )
                        .unsafeMap {
                            it.decodeFromJsonString()
                        }
                }
                dataToString = { it }
                notFoundErrorMessage = "There are no tags in ${props.testSuitesSource.gitDto.url}"
                this.selectedValue = selectedValue ?: ""
                onChangeFun = { tag ->
                    setSelectedValue(tag)
                }
            }
        }
        TestSuitesSourceFetchMode.BY_BRANCH -> div {
            branchSelector {
                formType = InputTypes.SOURCE_BRANCH
                validInput = true
                classes = "mb-2"
                formName = "Source branch:"
                getData = {
                    get(
                        url = "$urlPrefix/branch-list-to-fetch",
                        headers = jsonHeaders,
                        loadingHandler = ::loadingHandler,
                    )
                        .unsafeMap {
                            it.decodeFromJsonString()
                        }
                }
                dataToString = { it }
                notFoundErrorMessage = "There are no branches in ${props.testSuitesSource.gitDto.url}"
                this.selectedValue = selectedValue ?: ""
                onChangeFun = { tag ->
                    setSelectedValue(tag)
                }
            }
        }
        TestSuitesSourceFetchMode.BY_COMMIT -> div {
            inputTextFormRequired {
                form = InputTypes.SOURCE_COMMIT
                textValue = null
                validInput = true
                classes = "mb-2"
                name = "Commit (sha-1):"
                conflictMessage = null
                onChangeFun = setSelectedValue.fromInput()
            }
        }
    }
}
