@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "WildcardImport",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.frontend.externals.fontawesome.*
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.testsuite.*
import com.saveourtool.save.utils.prettyPrint
import com.saveourtool.save.utils.secondsToInstant

import csstype.ClassName
import csstype.Cursor
import js.core.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.label
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul

/**
 * Display single TestSuiteSource as list option
 *
 * @param isSelected flag that defines if this test suite source is selected or not
 * @param testSuitesSourceDtoWithId
 * @param selectHandler callback invoked on TestSuitesSource selection
 * @param editHandler callback invoked on edit TestSuitesSource button pressed
 * @param fetchHandler callback invoked on fetch button pressed
 * @param refreshHandler callback invoked on refresh button pressed
 */
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_PARAMETERS",
)
fun ChildrenBuilder.showTestSuitesSourceAsListElement(
    testSuitesSourceDtoWithId: TestSuitesSourceDtoWithId,
    isSelected: Boolean,
    selectHandler: (TestSuitesSourceDtoWithId) -> Unit,
    editHandler: (TestSuitesSourceDtoWithId) -> Unit,
    fetchHandler: (TestSuitesSourceDto) -> Unit,
    refreshHandler: () -> Unit,
) {
    val active = if (isSelected) "list-group-item-secondary" else ""
    li {
        className = ClassName("list-group-item $active")
        div {
            className = ClassName("d-flex w-100 justify-content-between")
            button {
                className = ClassName("btn btn-lg btn-link p-0 mb-1")
                onClick = {
                    selectHandler(testSuitesSourceDtoWithId)
                }
                label {
                    style = jso {
                        cursor = "pointer".unsafeCast<Cursor>()
                    }
                    fontAwesomeIcon(
                        if (isSelected) {
                            faArrowLeft
                        } else {
                            faArrowRight
                        }
                    )
                    +("  ${testSuitesSourceDtoWithId.content.name}")
                }
            }

            buttonBuilder(faEdit, null, title = "Edit source") {
                editHandler(testSuitesSourceDtoWithId)
            }
        }
        div {
            p {
                +(testSuitesSourceDtoWithId.content.description ?: "Description is not provided.")
            }
        }
        div {
            className = ClassName("clearfix")
            div {
                className = ClassName("float-left")
                buttonBuilder("Fetch new version", "info", isOutline = true, classes = "btn-sm mr-2") {
                    fetchHandler(testSuitesSourceDtoWithId.content)
                }
            }
            if (isSelected) {
                div {
                    className = ClassName("float-left")
                    buttonBuilder("Refresh", "info", isOutline = false, classes = "btn-sm mr-2") {
                        refreshHandler()
                    }
                }
            }
            span {
                className = ClassName("float-right align-bottom")
                asDynamic()["data-toggle"] = "tooltip"
                asDynamic()["data-placement"] = "bottom"
                title = "Organization-creator"
                +(testSuitesSourceDtoWithId.content.organizationName)
            }
        }
    }
}

/**
 * Display list of TestSuiteSources as a list
 *
 * @param testSuitesSources [TestSuitesSourceDtoWithIdList]
 * @param selectHandler callback invoked on TestSuitesSource selection
 * @param editHandler callback invoked on edit TestSuitesSource button pressed
 * @param fetchHandler callback invoked on fetch button pressed
 * @param refreshHandler
 */
fun ChildrenBuilder.showTestSuitesSources(
    testSuitesSources: TestSuitesSourceDtoWithIdList,
    selectHandler: (TestSuitesSourceDtoWithId) -> Unit,
    fetchHandler: (TestSuitesSourceDto) -> Unit,
    editHandler: (TestSuitesSourceDtoWithId) -> Unit,
    refreshHandler: () -> Unit,
) {
    div {
        className = ClassName("list-group")
        testSuitesSources.forEach {
            showTestSuitesSourceAsListElement(it, false, selectHandler, editHandler, fetchHandler, refreshHandler)
        }
    }
}

/**
 * Display list of [TestSuitesSourceSnapshotKey] of [selectedTestSuiteSource]
 *
 * @param selectedTestSuiteSource
 * @param testSuitesSourcesSnapshotKeys
 * @param selectHandler callback invoked on TestSuitesSource selection
 * @param editHandler callback invoked on edit TestSuitesSource button pressed
 * @param fetchHandler callback invoked on fetch button pressed
 * @param deleteHandler callback invoked on [TestSuitesSourceSnapshotKey] deletion
 * @param refreshHandler
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.showTestSuitesSourceSnapshotKeys(
    selectedTestSuiteSource: TestSuitesSourceDtoWithId,
    testSuitesSourcesSnapshotKeys: TestSuitesSourceSnapshotKeyList,
    selectHandler: (TestSuitesSourceDtoWithId) -> Unit,
    editHandler: (TestSuitesSourceDtoWithId) -> Unit,
    fetchHandler: (TestSuitesSourceDto) -> Unit,
    deleteHandler: (TestSuitesSourceSnapshotKey) -> Unit,
    refreshHandler: () -> Unit,
) {
    ul {
        className = ClassName("list-group")
        showTestSuitesSourceAsListElement(selectedTestSuiteSource, true, selectHandler, editHandler, fetchHandler, refreshHandler)
        if (testSuitesSourcesSnapshotKeys.isEmpty()) {
            li {
                className = ClassName("list-group-item list-group-item-light")
                +"This source is not fetched yet..."
            }
        } else {
            li {
                className = ClassName("list-group-item list-group-item-light")
                div {
                    className = ClassName("clearfix")
                    div {
                        className = ClassName("float-left")
                        +"Version"
                    }
                    div {
                        className = ClassName("float-right")
                        +"Git commit time"
                    }
                }
            }
            testSuitesSourcesSnapshotKeys.forEach { testSuitesSourceSnapshotKey ->
                li {
                    className = ClassName("list-group-item")
                    div {
                        className = ClassName("clearfix")
                        div {
                            className = ClassName("float-left")
                            +testSuitesSourceSnapshotKey.version
                        }
                        buttonBuilder(faTimesCircle, style = null, classes = "float-right btn-sm pt-0 pb-0") {
                            deleteHandler(testSuitesSourceSnapshotKey)
                        }
                        div {
                            className = ClassName("float-right")
                            +testSuitesSourceSnapshotKey.creationTimeInMills.secondsToInstant().prettyPrint()
                        }
                    }
                }
            }
        }
    }
}
