package com.saveourtool.save.frontend.components.views.contests

import csstype.ClassName
import csstype.HtmlAttributes
import csstype.rem
import kotlinx.js.jso
import react.ChildrenBuilder
import react.dom.html.ReactHTML
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.p

fun ChildrenBuilder.proposeContest() {
    ReactHTML.div {
        className = ClassName("col-lg-2")
        ReactHTML.div {
            className = ClassName("card flex-md-row mb-1 box-shadow")
            style = jso {
                minHeight = 7.rem
            }

            // FixMe: Want to propose contest? Write e-mail! undraw_mailbox_re_dvds.svg
        }
    }

}