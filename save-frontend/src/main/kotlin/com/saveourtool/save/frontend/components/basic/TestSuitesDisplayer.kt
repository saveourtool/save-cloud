@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "WildcardImport",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.domain.pluginName
import com.saveourtool.save.frontend.components.basic.testsuiteselector.TestSuiteSelectorMode
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.GIT_HASH_PREFIX_LENGTH
import csstype.ClassName
import react.ChildrenBuilder
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.small

/**
 * @param testSuites
 * @param selectedTestSuites
 * @param displayMode if used not inside TestSuiteSelector, should be null, otherwise should be mode of TestSuiteSelector
 * @param onTestSuiteClick
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun ChildrenBuilder.showAvaliableTestSuites(
    testSuites: List<TestSuiteDto>,
    selectedTestSuites: List<TestSuiteDto>,
    displayMode: TestSuiteSelectorMode?,
    onTestSuiteClick: (TestSuiteDto) -> Unit,
) {
    div {
        className = ClassName("list-group")
        testSuites.forEach { testSuite ->
            val active = if (testSuite in selectedTestSuites) {
                "active"
            } else {
                ""
            }
            a {
                className = ClassName("list-group-item list-group-item-action $active")
                onClick = {
                    onTestSuiteClick(testSuite)
                }
                div {
                    className = ClassName("d-flex w-100 justify-content-between")
                    h5 {
                        className = ClassName("mb-1")
                        +(testSuite.name)
                    }
                    small {
                        +(testSuite.language ?: "")
                    }
                }
                div {
                    className = ClassName("clearfix mb-1")
                    div {
                        className = ClassName("float-left")
                        p {
                            +(testSuite.description ?: "")
                        }
                    }
                    div {
                        className = ClassName("float-right")
                        if (displayMode.shouldDisplayVersion()) {
                            small {
                                asDynamic()["data-toggle"] = "tooltip"
                                asDynamic()["data-placement"] = "bottom"
                                title = "Hash of commit with current test suite"
                                +testSuite.version.take(GIT_HASH_PREFIX_LENGTH)
                            }
                        }
                    }
                }
                div {
                    className = ClassName("clearfix")
                    small {
                        className = ClassName("float-left")
                        asDynamic()["data-toggle"] = "tooltip"
                        asDynamic()["data-placement"] = "bottom"
                        title = "Test suite tags"
                        +(testSuite.tags?.joinToString(", ") ?: "")
                    }

                    small {
                        className = ClassName("float-right")
                        asDynamic()["data-toggle"] = "tooltip"
                        asDynamic()["data-placement"] = "bottom"
                        title = "Plugin type"
                        +(testSuite.plugins.joinToString(", ") { it.pluginName() })
                    }
                }
            }
        }
    }
}

private fun TestSuiteSelectorMode?.shouldDisplayVersion() = this != null && this != TestSuiteSelectorMode.BROWSER
