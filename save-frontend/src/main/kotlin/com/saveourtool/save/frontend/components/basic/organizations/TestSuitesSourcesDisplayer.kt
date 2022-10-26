@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "WildcardImport",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic.organizations

import com.saveourtool.save.frontend.externals.fontawesome.faEdit
import com.saveourtool.save.frontend.externals.fontawesome.faTimesCircle
import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.testsuite.*

import csstype.ClassName
import csstype.Cursor
import react.ChildrenBuilder
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.span
import react.dom.html.ReactHTML.ul

import kotlinx.js.jso

/**
 * Display single TestSuiteSource as list option
 *
 * @param isSelected flag that defines if this test suite source is selected or not
 * @param testSuitesSourceDtoWithId
 * @param selectHandler callback invoked on TestSuitesSource selection
 * @param editHandler callback invoked on edit TestSuitesSource button pressed
 * @param fetchHandler callback invoked on fetch button pressed
 */
fun ChildrenBuilder.showTestSuitesSourceAsListElement(
    testSuitesSourceDtoWithId: TestSuitesSourceDtoWithId,
    isSelected: Boolean,
    selectHandler: (TestSuitesSourceDtoWithId) -> Unit,
    editHandler: (TestSuitesSourceDtoWithId) -> Unit,
    fetchHandler: (TestSuitesSourceDto) -> Unit,
) {
    val active = if (isSelected) "list-group-item-secondary" else ""
    li {
        className = ClassName("list-group-item $active")
        div {
            className = ClassName("d-flex w-100 justify-content-between")
            button {
                className = ClassName("btn btn-lg btn-link p-0 mb-1")
                style = jso {
                    cursor = "pointer".unsafeCast<Cursor>()
                }
                onClick = {
                    selectHandler(testSuitesSourceDtoWithId)
                }
                +(testSuitesSourceDtoWithId.content.name)
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
 */
fun ChildrenBuilder.showTestSuitesSources(
    testSuitesSources: TestSuitesSourceDtoWithIdList,
    selectHandler: (TestSuitesSourceDtoWithId) -> Unit,
    fetchHandler: (TestSuitesSourceDto) -> Unit,
    editHandler: (TestSuitesSourceDtoWithId) -> Unit,
) {
    div {
        className = ClassName("list-group")
        testSuitesSources.forEach {
            showTestSuitesSourceAsListElement(it, false, selectHandler, editHandler, fetchHandler)
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
 */
@Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
fun ChildrenBuilder.showTestSuitesSourceSnapshotKeys(
    selectedTestSuiteSource: TestSuitesSourceDtoWithId,
    testSuitesSourcesSnapshotKeys: TestSuitesSourceSnapshotKeyList,
    selectHandler: (TestSuitesSourceDtoWithId) -> Unit,
    editHandler: (TestSuitesSourceDtoWithId) -> Unit,
    fetchHandler: (TestSuitesSourceDto) -> Unit,
    deleteHandler: (TestSuitesSourceSnapshotKey) -> Unit,
) {
    ul {
        className = ClassName("list-group")
        showTestSuitesSourceAsListElement(selectedTestSuiteSource, true, selectHandler, editHandler, fetchHandler)
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
                        +"Upload time"
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
                        buttonBuilder(faTimesCircle, style = null, classes = "float-right btn-sm") {
                            deleteHandler(testSuitesSourceSnapshotKey)
                        }
                        div {
                            className = ClassName("float-right")
                            +testSuitesSourceSnapshotKey.creationTimeInMills.toString()
                        }
                    }
                }
            }
        }
    }
}
