/**
 * This card provides a global information about SaveOurTool
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.components.basic.markdown
import js.core.jso
import react.FC
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.i
import react.dom.html.ReactHTML.p
import web.cssom.ClassName
import web.cssom.TextAlign

private const val SERVICES = """
    provides Intelligent Services for developers of code analysis tools. Our two main directions:
"""

private const val SAVE = """
     - a platform for a distributed Cloud CI of code analyzers with a special test framework. With SAVE you can:
"""

private const val SAVE_POSSIBILITIES = """
    |- quickly establish testing and CI of your analyzer; 
    |- share your tests with community to compare other tools with your tool;
    |- using SAVE you can even create an online demo for your analyzer and setup it for your community.
"""

private const val VULN = """
    - a platform for reporting, aggregation and dedublication of 1-day Vulerabilities.
"""

private const val CONTESTS = """
    Also we establish contests in the area of code analysis where you can propose your automated solutions for 
    finding bugs and compete with other projects.
"""

val cardServiceInfo: FC<IndexViewProps> = FC { props ->
    div {
        className = ClassName("col-3 shadow mx-3 mt-2")
        div {
            className = ClassName("row d-flex justify-content-center")
            cardImage("/img/icon2.png")
        }

        div {
            className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")

            h5 {
                style = jso {
                    textAlign = TextAlign.center
                }
                +"Multiple different services"
            }
        }

        div {
            className = ClassName("row")
            div {
                className = ClassName("col-12")
                p {
                    b {
                        +"SaveOurTool "
                    }
                    +SERVICES
                }
                p {
                    +"1. "
                    b {
                        i {
                            +"SAVE "
                        }
                    }
                    +SAVE
                }
                p {
                    markdown(SAVE_POSSIBILITIES.trimMargin())
                }
                p {
                    +"2. "
                    b {
                        i {
                            +"VULN "
                        }
                    }
                    +VULN
                }
                p {
                    className = ClassName("text-gray-700")
                    +CONTESTS
                }
            }
        }
    }
}
