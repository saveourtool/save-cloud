/**
 * This card provides a global information about SaveOurTool
 */

package com.saveourtool.save.frontend.components.views.index

import js.core.jso
import react.FC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import web.cssom.ClassName
import web.cssom.TextAlign

// FixMe: all links with descriptions in pretty format

private const val SERVICES = """
    SaveOurTool provides Intelligent Services for developers of code analysis tools. Our two main directions:
    1. SAVE - a platform for a distributed Cloud CI of code analyzers with a special test framework. With SAVE you can:
    quickly establish testing and CI of your analyzer, share your tests with community to compare other tools with your tool
    using your benchmarks. Using SAVE you can even create an online demo for your analyzer and setup it for your community.
    2. VULN - a platform for reporting, aggregation and dedublication of 1-day Vulerabilities.
    
    Also we establish contests in the area of code analysis where you can propose your automated solutions for 
    finding bugs and compete with other projects.
"""

val cardServiceInfo: FC<IndexViewProps> = FC { props ->
    div {
        className = ClassName("col-3 mx-2 mt-2")
        div {
            className = ClassName("row d-flex justify-content-center")
            cardImage("img/icon2.png")
        }

        div {
            className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")

            ReactHTML.h5 {
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
                    +SERVICES
                }
            }
        }
    }
}
