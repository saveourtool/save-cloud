package com.saveourtool.save.frontend.components.basic.testsuitessources.fetch

import com.saveourtool.save.frontend.components.modal.largeTransparentModalStyle
import com.saveourtool.save.frontend.components.modal.modal
import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.WindowOpenness
import com.saveourtool.save.frontend.utils.buttonWithIcon
import com.saveourtool.save.frontend.utils.withUnusedArg
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import csstype.ClassName
import react.ChildrenBuilder
import react.FC
import react.Props
import react.dom.aria.ariaLabel
import react.dom.html.ButtonType
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.useState

external interface TestSuitesSourceFetcherProps : Props {
    /**
     * Control openness of modal window
     */
    var windowOpenness: WindowOpenness

    /**
     * [TestSuitesSourceDto] to be fetched
     */
    var testSuitesSource: TestSuitesSourceDto
}

/**
 * Enum that represents different modes of []
 */
enum class TestSuitesSourceFetcherMode {
    BY_TAG,
    BY_BRANCH,
    BY_COMMIT,
}

fun ChildrenBuilder.testSuitesSourceFetcher(
    windowOpenness: WindowOpenness,
    testSuitesSource: TestSuitesSourceDto,
) {
    console.log("render testSuitesSourceFetcher")
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
                    }
                }

                div {
                    className = ClassName("modal-footer")
                    div {
                        className = ClassName("d-flex justify-content-center")
                        ReactHTML.button {
                            type = ButtonType.button
                            className = ClassName("btn btn-primary mt-4")
                            +"Fetch"
                            onClick = {
                                console.log("fetch is runne")
                            }
                        }
                    }
                    div {
                        className = ClassName("d-flex justify-content-center")
                        ReactHTML.button {
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

private val innerTestSuitesSourceFetcher = innerTestSuitesSourceFetcher()

private fun innerTestSuitesSourceFetcher() = FC<TestSuitesSourceFetcherProps> { props ->
    val currentModeState = useState(TestSuitesSourceFetcherMode.BY_TAG)
    val (currentMode, setCurrentMode) = currentModeState

    div {
        className = ClassName("d-flex align-self-center justify-content-around mb-2")
        buttonWithIcon(
            icon = faTag,
            tooltipText = "Fetch test suites source by tag",
            buttonMode = TestSuitesSourceFetcherMode.BY_TAG,
            currentModeState = currentModeState
        )
        buttonWithIcon(
            icon = faCodeBranch,
            tooltipText = "Fetch test suites source by branch",
            buttonMode = TestSuitesSourceFetcherMode.BY_BRANCH,
            currentModeState = currentModeState
        )
        buttonWithIcon(
            icon = faCheckCircle,
            tooltipText = "Fetch test suites source by commit",
            buttonMode = TestSuitesSourceFetcherMode.BY_COMMIT,
            currentModeState = currentModeState
        )
    }
}
