package com.saveourtool.save.frontend.components.views.welcome

import com.saveourtool.save.frontend.components.views.AbstractView
import com.saveourtool.save.frontend.components.views.welcome.pagers.allWelcomePagers
import com.saveourtool.save.frontend.externals.animations.animator
import com.saveourtool.save.frontend.externals.animations.scrollContainer
import com.saveourtool.save.frontend.externals.animations.scrollPage
import com.saveourtool.save.frontend.externals.fontawesome.faChevronDown
import com.saveourtool.save.frontend.externals.fontawesome.fontAwesomeIcon
import csstype.*
import js.core.jso
import react.dom.html.*
import react.ChildrenBuilder
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.b
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h2
import react.dom.html.ReactHTML.h4
import react.dom.html.ReactHTML.img

class WelcomeViewMobile : AbstractView<WelcomeProps, IndexViewState>(false) {

    override fun ChildrenBuilder.render() {
        sorryYourScreenIsTooSmall()
    }
}

private fun ChildrenBuilder.sorryYourScreenIsTooSmall() {
        @Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
        scrollContainer {
            scrollPage {
                notSupportedMobileYet()
                title()
                chevron()
            }

            allWelcomePagers.forEach { pager ->
                scrollPage { }
                pager.forEach {
                    scrollPage {
                        div {
                            animator {
                                animation = it.animation
                                it.renderPage(this)
                            }
                        }
                    }
                }
            }
        }
}

private fun ChildrenBuilder.notSupportedMobileYet() {
    div {
        className = ClassName("row d-flex justify-content-center mx-auto text-danger mt-2")
        h4 {
            className = ClassName("text-center")
            +"We do not "
            b {
                +"yet "
            }
            +"support devices with small screen size"
        }
    }
    div {
        className = ClassName("row d-flex justify-content-center mx-auto")
        img {
            src = "img/sad_cat.gif"
        }
    }
}

private fun ChildrenBuilder.title() {
    div {
        className = ClassName("row justify-content-center mx-auto")
        // Marketing information
        welcomeMarketingTitle("text-primary", true)
    }
}

private fun ChildrenBuilder.chevron() {
    div {
        className = ClassName("row justify-content-center")
        h2 {
            className = ClassName("animate__animated animate__pulse animate__infinite")
            style = jso {
                fontSize = 3.rem
                color = "rgb(6, 7, 89)".unsafeCast<Color>()
            }
            fontAwesomeIcon(faChevronDown)
        }
    }
}

