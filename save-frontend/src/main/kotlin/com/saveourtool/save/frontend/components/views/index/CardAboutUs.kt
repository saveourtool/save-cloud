/**
 * Just info about us on welcome page
 */

package com.saveourtool.save.frontend.components.views.index

import com.saveourtool.save.frontend.utils.buttonBuilder
import com.saveourtool.save.validation.FrontendRoutes
import js.core.jso
import react.FC
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.router.Navigate
import react.router.dom.Link
import web.cssom.ClassName
import web.cssom.TextAlign

// FixMe: Add links to our projects save-cloud, save-cli with logo (to Github)

private const val WHO_ARE_WE = """
    We are just a group of several developers working on this community project. 
    Our main idea is that we can unify together all routine work that is done in the area of code analysis and help 
    developers of analyzers focus on their primary work: find bugs in code.
"""

val cardAboutUs: FC<IndexViewProps> = FC { props ->
    div {
        className = ClassName("col-3 mx-2 mt-2")
        div {
            className = ClassName("row d-flex justify-content-center")
            cardImage("img/icon3.png")
        }

        div {
            className = ClassName("row d-flex justify-content-center text-gray-900 mt-2")

            ReactHTML.h5 {
                style = jso {
                    textAlign = TextAlign.center
                }
                +"About Us"
            }
        }

        div {
            className = ClassName("row d-flex justify-content-center")
            div {
                className = ClassName("col-6")
                p {
                    +WHO_ARE_WE
                }
                div {
                    className = ClassName("row d-flex justify-content-center mt-1")
                    buttonBuilder(
                        "About us",
                        style = "secondary rounded-pill",
                        isOutline = false
                    ) {
                        Navigate {
                            to = "/${FrontendRoutes.ABOUT_US}"
                        }
                    }
                }
            }

            div {
                className = ClassName("col-6")
                p {
                    +"We kindly ask you not to break this service and report any problems that you will find to our Github. Please also read our"
                    Link {
                        +" Terms of Usage"
                        to = "/${FrontendRoutes.TERMS_OF_USE}"
                    }
                    +"."
                }
            }
        }
    }
}
