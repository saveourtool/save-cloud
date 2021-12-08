/**
 * Grid with configurable number of checkboxes
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")

package org.cqfn.save.frontend.components.basic

import org.cqfn.save.frontend.externals.fontawesome.faQuestionCircle
import org.cqfn.save.frontend.externals.fontawesome.fontAwesomeIcon
import org.cqfn.save.testsuite.TestSuiteDto

import react.PropsWithChildren
import react.fc
import react.useEffect

import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onClickFunction
import react.dom.*

/**
 * Props for ChecboxGrid component
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
}

fun suitesTable(
        suites: List<TestSuiteDto>,
        selectedLanguageForStandardTests: String?,
        setSelectedLanguageForStandardTests: (String) -> Unit,
) = fc<CheckBoxGridProps> { props ->
    nav("nav nav-tabs mb-4") {
        val languages = suites.map { it.language }.distinct()
        languages.forEachIndexed { index, langStr ->
            val lang = langStr?.trim() ?: "Other"
            li("nav-item") {
                p {
                    attrs["class"] = "nav-link"
                    attrs.onClickFunction = {
                        setSelectedLanguageForStandardTests(lang)
                    }

                    val languageWasNotSelected = (selectedLanguageForStandardTests.isNullOrBlank() && index == 0)
                    if (languageWasNotSelected) { setSelectedLanguageForStandardTests(lang) }
                    if (languageWasNotSelected || lang == selectedLanguageForStandardTests) {
                        attrs["class"] = "${attrs["class"]} active font-weight-bold text-gray-800"
                    }

                    +lang
                }
            }
        }
    }
}

/**
 * @param suites a list of [TestSuiteDto]s which should be displayed on the grid
 * @return an RComponent
 */
@Suppress("TOO_LONG_FUNCTION", "LongMethod")
fun checkBoxGrid(suites: List<TestSuiteDto>, selectedLanguageForStandardTests: String?) =
        fc<CheckBoxGridProps> { props ->
            div {
                suites.chunked(props.rowSize)
                        .forEach { row ->
                            div("row g-3") {
                                row.forEach { suite ->
                                    // display only those tests that are related to the proper language
                                    if ((suite.language?.trim() ?: "Other") == selectedLanguageForStandardTests) {
                                        div("col-md-6") {
                                            input(type = InputType.checkBox, classes = "mr-2") {
                                                attrs.defaultChecked = props.selectedStandardSuites.contains(suite.name)
                                                attrs.onClickFunction = {
                                                    if (props.selectedStandardSuites.contains(suite.name)) {
                                                        props.selectedStandardSuites.remove(suite.name)
                                                    } else {
                                                        props.selectedStandardSuites.add(suite.name)
                                                    }
                                                }
                                            }

                                            val suiteName = suite.name.replaceFirstChar { it.uppercaseChar() }
                                            +if (suiteName.length > 20) "${suiteName.take(20)}..." else suiteName

                                            sup("tooltip-and-popover ml-1") {
                                                fontAwesomeIcon(icon = faQuestionCircle)
                                                attrs["tooltip-placement"] = "top"
                                                attrs["tooltip-title"] = suite.description?.take(100) ?: ""
                                                attrs["popover-placement"] = "right"
                                                attrs["popover-title"] = suite.name
                                                attrs["popover-content"] = suiteDescription(suite)
                                            }
                                        }
                                    }
                                }
                            }
                        }
            }
            useEffect(emptyList<dynamic>()) {
                js("var jQuery = require(\"jquery\")")
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
