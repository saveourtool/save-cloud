@file:Suppress(
    "FILE_NAME_MATCH_CLASS",
    "FILE_WILDCARD_IMPORTS",
    "WildcardImport",
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
)

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.components.basic.testsuiteselector.TestSuiteSelectorMode
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteVersioned
import com.saveourtool.save.utils.PRETTY_DELIMITER
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
fun ChildrenBuilder.showAvailableTestSuites(
    testSuites: List<TestSuiteVersioned>,
    selectedTestSuites: List<TestSuiteVersioned>,
    displayMode: TestSuiteSelectorMode?,
    onTestSuiteClick: (TestSuiteVersioned) -> Unit,
) {
    doShowAvailableTestSuites(
        testSuites,
        onTestSuiteClick,
        isSelected = { it in selectedTestSuites },
        nameGetter = TestSuiteVersioned::name,
        languageGetter = TestSuiteVersioned::language,
        descriptionGetter = TestSuiteVersioned::description,
        versionGetter = { testSuite -> testSuite.version.takeIf { displayMode.shouldDisplayVersion() } },
        tagsGetter = TestSuiteVersioned::tags,
        pluginsGetter = TestSuiteVersioned::plugins,
    )
}

/**
 * @param testSuites
 * @param selectedTestSuiteId
 * @param onTestSuiteClick
 */
fun ChildrenBuilder.showAvailableTestSuites(
    testSuites: List<TestSuiteDto>,
    selectedTestSuiteId: Long?,
    onTestSuiteClick: (TestSuiteDto) -> Unit,
) {
    doShowAvailableTestSuites(
        testSuites,
        onTestSuiteClick,
        isSelected = { it.requiredId() == selectedTestSuiteId },
        nameGetter = TestSuiteDto::name,
        languageGetter = { it.language.orEmpty() },
        descriptionGetter = { it.description.orEmpty() },
        versionGetter = { null },
        tagsGetter = { it.tags?.joinToString(PRETTY_DELIMITER).orEmpty() },
        pluginsGetter = { it.plugins.joinToString(PRETTY_DELIMITER) },
    )
}

@Suppress("TOO_LONG_FUNCTION", "LongMethod", "TOO_MANY_PARAMETERS", "LongParameterList")
private fun <T> ChildrenBuilder.doShowAvailableTestSuites(
    testSuites: List<T>,
    onTestSuiteClick: (T) -> Unit,
    isSelected: (T) -> Boolean,
    nameGetter: (T) -> String,
    languageGetter: (T) -> String,
    descriptionGetter: (T) -> String,
    versionGetter: (T) -> String?,
    tagsGetter: (T) -> String,
    pluginsGetter: (T) -> String,
) {
    div {
        className = ClassName("list-group")
        testSuites.forEach { testSuite ->
            val active = if (isSelected(testSuite)) {
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
                        +nameGetter(testSuite)
                    }
                    small {
                        +languageGetter(testSuite)
                    }
                }
                div {
                    className = ClassName("clearfix mb-1")
                    div {
                        className = ClassName("float-left")
                        p {
                            +descriptionGetter(testSuite)
                        }
                    }
                    div {
                        className = ClassName("float-right")
                        val version = versionGetter(testSuite)
                        version?.run {
                            small {
                                asDynamic()["data-toggle"] = "tooltip"
                                asDynamic()["data-placement"] = "bottom"
                                title = "Hash of commit/branch name/tag name"
                                +this
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
                        +tagsGetter(testSuite)
                    }

                    small {
                        className = ClassName("float-right")
                        asDynamic()["data-toggle"] = "tooltip"
                        asDynamic()["data-placement"] = "bottom"
                        title = "Plugin type"
                        +pluginsGetter(testSuite)
                    }
                }
            }
        }
    }
}

private fun TestSuiteSelectorMode?.shouldDisplayVersion() = this != null && this != TestSuiteSelectorMode.BROWSER
