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
import react.dom.html.ReactHTML

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
    ReactHTML.div {
        className = ClassName("list-group")
        testSuites.forEach { testSuite ->
            val active = if (testSuite in selectedTestSuites) {
                "active"
            } else {
                ""
            }
            ReactHTML.a {
                className = ClassName("list-group-item list-group-item-action $active")
                onClick = {
                    onTestSuiteClick(testSuite)
                }
                ReactHTML.div {
                    className = ClassName("d-flex w-100 justify-content-between")
                    ReactHTML.h5 {
                        className = ClassName("mb-1")
                        +(testSuite.name)
                    }
                    ReactHTML.small {
                        +(testSuite.language ?: "")
                    }
                }
                ReactHTML.p {
                    +(testSuite.description ?: "")
                }
                ReactHTML.small {
                    +(testSuite.tags?.joinToString(", ") ?: "")
                }
            }
        }
    }
}
