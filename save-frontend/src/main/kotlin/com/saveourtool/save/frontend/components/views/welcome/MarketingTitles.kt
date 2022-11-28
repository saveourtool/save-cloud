package com.saveourtool.save.frontend.components.views.welcome

import csstype.*
import js.core.jso

import react.ChildrenBuilder
import react.dom.html.ReactHTML

fun ChildrenBuilder.welcomeMarketingTitle(textColor: String, isDark: Boolean = false) {
    ReactHTML.div {
        className = ClassName("col-lg-4 ml-auto mt-3 mb-5 mr-5 ml-0 $textColor")
        marketingTitle("Software", isDark)
        marketingTitle("Analysis", isDark)
        marketingTitle("Verification &", isDark)
        marketingTitle("Evaluation", isDark)
        ReactHTML.h3 {
            if (isDark) {
                style = jso {
                    color = "rgb(6, 7, 89)".unsafeCast<Color>()
                }
            }
            className = ClassName("mt-4")
            +"Advanced open-source cloud eco-system for continuous integration, evaluation and benchmarking of software tools."
        }
    }
}

fun ChildrenBuilder.marketingTitle(str: String, isDark: Boolean) {
    ReactHTML.div {
        if (isDark) {
            style = jso {
                color = "rgb(6, 7, 89)".unsafeCast<Color>()
            }
        }
        className = ClassName("mb-0 mt-0")
        h1Bold(str[0].toString())
        h1Normal(str.substring(1, str.length))
    }
}

private fun ChildrenBuilder.h1Bold(str: String) = ReactHTML.h1 {
    +str
    style = jso {
        fontWeight = "bold".unsafeCast<FontWeight>()
        display = Display.inline
        fontSize = "4.5rem".unsafeCast<FontSize>()
    }
}

private fun ChildrenBuilder.h1Normal(str: String) = ReactHTML.h1 {
    +str
    style = jso {
        display = Display.inline
    }
}
