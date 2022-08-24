@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "WildcardImport",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.testsuite.TestSuiteDto
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
 * @param onTestSuiteClick
 */
fun ChildrenBuilder.showAvaliableTestSuites(
    testSuites: List<TestSuiteDto>,
    selectedTestSuites: List<TestSuiteDto>,
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
                p {
                    +(testSuite.description ?: "")
                }
                div {
                    className = ClassName("d-flex justify-content-between")
                    small {
                        +(testSuite.tags?.joinToString(", ") ?: "")
                    }

                    small {
                        +(testSuite.plugins.joinToString(", ") { it.pluginName })
                    }
                }
            }
        }
    }
}
