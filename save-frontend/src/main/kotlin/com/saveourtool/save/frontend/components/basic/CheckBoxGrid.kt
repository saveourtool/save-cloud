/**
 * Grid with configurable number of checkboxes
 */

@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "WildcardImport")

package com.saveourtool.save.frontend.components.basic

import com.saveourtool.save.frontend.externals.fontawesome.faQuestionCircle
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import com.saveourtool.save.testsuite.TestSuiteDto

import csstype.ClassName
import react.FC
import react.PropsWithChildren
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.nav
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.sup
import react.useEffect

/**
 * Props for CheckboxGrid component
 */
external interface CheckBoxGridProps : PropsWithChildren {
    /**
     * Length of row of checkboxes
     */
    var rowSize: Int

    /**
     * Currently selected elements
     */
    var selectedStandardSuites: MutableList<String>

    /**
     * A list of [TestSuiteDto]s which should be displayed on the grid
     */
    var suites: List<TestSuiteDto>

    /**
     * Language selected for standard tests
     */
    var selectedLanguageForStandardTests: String?
}

/**
 * @param suites
 * @param selectedLanguageForStandardTests
 * @param setSelectedLanguageForStandardTests
 * @return functional interface with navigation menu
 */
fun suitesTable(
    suites: List<TestSuiteDto>,
    selectedLanguageForStandardTests: String?,
    setSelectedLanguageForStandardTests: (String) -> Unit,
) = FC<CheckBoxGridProps> {
    nav {
        className = ClassName("nav nav-tabs mb-4")
        val (languagesWithoutNull, otherLanguages) = suites.map { it.language }
            .distinct()
            .sortedBy { it }
            .partition { it != null }
        val languages = languagesWithoutNull.toMutableList()
        if (otherLanguages.isNotEmpty()) {
            languages.add(null)
        }
        languages.forEachIndexed { index, langStr ->
            val lang = langStr?.trim() ?: "Other"
            li {
                className = ClassName("nav-item")
                p {
                    className = ClassName("nav-link")
                    onClick = {
                        setSelectedLanguageForStandardTests(lang)
                    }

                    val languageWasNotSelected = (selectedLanguageForStandardTests.isNullOrBlank() && index == 0)
                    if (languageWasNotSelected) {
                        setSelectedLanguageForStandardTests(lang)
                    }
                    if (languageWasNotSelected || lang == selectedLanguageForStandardTests) {
                        className = ClassName("$className active font-weight-bold text-gray-800")
                    }

                    +lang
                }
            }
        }
    }
}

/**
 * @return a [FC]
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun checkBoxGrid() =
        FC<CheckBoxGridProps> { props ->
            div {
                className = ClassName("mt-3")
                props.suites.chunked(props.rowSize)
                    .forEach { row ->
                        div {
                            className = ClassName("row g-3")
                            row.forEach { suite ->
                                // display only those tests that are related to the proper language
                                if ((suite.language?.trim() ?: "Other") == props.selectedLanguageForStandardTests) {
                                    div {
                                        className = ClassName("col-md-6")
                                        input {
                                            type = InputType.checkbox
                                            className = ClassName("mr-2")
                                            defaultChecked = props.selectedStandardSuites.contains(suite.name)
                                            onClick = {
                                                if (props.selectedStandardSuites.contains(suite.name)) {
                                                    props.selectedStandardSuites.remove(suite.name)
                                                } else {
                                                    props.selectedStandardSuites.add(suite.name)
                                                }
                                            }
                                        }

                                        val suiteName = suite.name.replaceFirstChar { it.uppercaseChar() }
                                        +if (suiteName.length > 11) "${suiteName.take(11)}..." else suiteName

                                        sup {
                                            className = ClassName("tooltip-and-popover ml-1")
                                            fontAwesomeIcon(icon = faQuestionCircle)
                                            tabIndex = 0
                                            asDynamic()["tooltip-placement"] = "top"
                                            asDynamic()["tooltip-title"] = ""
                                            asDynamic()["popover-placement"] = "right"
                                            asDynamic()["popover-title"] = suite.name
                                            asDynamic()["popover-content"] = suiteDescription(suite)
                                            asDynamic()["data-trigger"] = "focus"
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
            useEffect {
                js("var jQuery = require(\"jquery\")")
                js("require(\"popper.js\")")
                js("require(\"bootstrap\")")
                js("""jQuery('.tooltip-and-popover').each(function() {
            jQuery(this).popover({
                placement: jQuery(this).attr("popover-placement"),
                title: jQuery(this).attr("popover-title"),
                content: jQuery(this).attr("popover-content"),
                html: true
            }).tooltip({
                placement: jQuery(this).attr("tooltip-placement"), 
                title: jQuery(this).attr("tooltip-title")
            }).on('show.bs.popover', function() {
                jQuery(this).tooltip('hide')
            }).on('hide.bs.popover', function() {
                jQuery(this).tooltip('show')
            })
        })""")
            }
        }
